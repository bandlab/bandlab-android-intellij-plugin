#!/usr/bin/env python3
# Copyright 2026 BandLab Singapore Pte Ltd
# SPDX-License-Identifier: Apache-2.0
"""Add or replace a plugin version in a custom plugin repository index (updatePlugins.xml).

Developers' IDEs point at the index on Artifactory; the plugin zip itself lives on Maven
Central. The entry is built from META-INF/plugin.xml inside the published zip, so the index
always describes exactly what was published. The entry with the same plugin id and IDE build
range is replaced (the IDE offers the newest version per range); other entries are kept.

Usage: update_plugin_index.py <plugin.zip> <download-url> <index-in> <index-out>
"""

import io
import sys
import xml.etree.ElementTree as ET
import zipfile


def read_plugin_xml(zip_path):
    """Returns the root of META-INF/plugin.xml from the plugin jar inside the zip."""
    with zipfile.ZipFile(zip_path) as plugin_zip:
        for name in plugin_zip.namelist():
            if "/lib/" not in name or not name.endswith(".jar"):
                continue
            with zipfile.ZipFile(io.BytesIO(plugin_zip.read(name))) as jar:
                if "META-INF/plugin.xml" in jar.namelist():
                    return ET.fromstring(jar.read("META-INF/plugin.xml"))
    raise SystemExit(f"No META-INF/plugin.xml found in {zip_path}")


def normalize_build(build):
    """'253', '253.0' and '253.*' all mean the same build range boundary."""
    if not build:
        return ""
    parts = build.split(".")
    while len(parts) > 1 and parts[-1] in ("0", "*"):
        parts.pop()
    return ".".join(parts)


def build_range(element):
    idea_version = element.find("idea-version")
    if idea_version is None:
        return "", ""
    return (
        normalize_build(idea_version.get("since-build")),
        normalize_build(idea_version.get("until-build")),
    )


def build_entry(plugin_xml, download_url):
    plugin_id = plugin_xml.findtext("id")
    version = plugin_xml.findtext("version")
    if not plugin_id or not version:
        raise SystemExit("plugin.xml is missing <id> or <version>")

    entry = ET.Element("plugin", {"id": plugin_id, "url": download_url, "version": version})
    idea_version = plugin_xml.find("idea-version")
    if idea_version is not None:
        ET.SubElement(entry, "idea-version", dict(idea_version.attrib))
    for tag in ("name", "description", "change-notes"):
        text = plugin_xml.findtext(tag)
        if text:
            ET.SubElement(entry, tag).text = text
    return entry


def update_index(index_root, entry):
    key = (entry.get("id"), build_range(entry))
    for i, existing in enumerate(index_root.findall("plugin")):
        if (existing.get("id"), build_range(existing)) == key:
            index_root.remove(existing)
            index_root.insert(i, entry)
            return "Replaced"
    index_root.append(entry)
    return "Added"


def main(zip_path, download_url, index_in, index_out):
    entry = build_entry(read_plugin_xml(zip_path), download_url)
    tree = ET.parse(index_in)
    action = update_index(tree.getroot(), entry)
    ET.indent(tree, space="    ")
    tree.write(index_out, encoding="utf-8", xml_declaration=True)
    since, until = build_range(entry)
    print(
        f"{action} {entry.get('id')} {entry.get('version')} "
        f"(builds {since or '*'} - {until or '*'}) -> {download_url}",
    )


if __name__ == "__main__":
    if len(sys.argv) != 5:
        raise SystemExit(__doc__)
    main(*sys.argv[1:])
