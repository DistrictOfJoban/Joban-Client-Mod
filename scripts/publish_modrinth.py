#!/usr/bin/env python3
"""Publish the existing 2.3.0-1.0.0 GitHub Release jars to Modrinth v2.

No build commands, edits to existing versions, or automatic POST retries.
API reference: https://docs.modrinth.com/api/operations/createversion/
The workflow must serialize publishers for this project; the API has no
client-supplied idempotency key. An uncertain POST is reconciled by GET only.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
import hashlib
import http.client
import json
import os
from pathlib import Path
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid


VERSION = "2.3.0-1.0.0"
TAG = "v" + VERSION
REPOSITORY = "https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang"
API = "https://api.modrinth.com/v2"
USER_AGENT = "BugQuantumCore/Joban-Client-Mod-Yanyang-publisher/1.0 (github.com/BugQuantumCore/Joban-Client-Mod-Yanyang)"
MINECRAFT_VERSIONS = ("1.17.1", "1.18.2", "1.19.2", "1.19.4", "1.20.1", "1.20.4")
LOADERS = ("fabric", "forge")
DEPENDENCIES = {
    "mtr": ("minecraft-transit-railway", "XKPAmI6u"),
    "fabric_api": ("fabric-api", "P7dR8mSH"),
    "jcm": ("jcm", "lQqKCDhg"),
}


class PublishError(RuntimeError):
    """A precondition, metadata conflict or known publication failure."""


class UncertainUpload(PublishError):
    """A POST may have committed; only read-only reconciliation is safe."""


@dataclass(frozen=True)
class Artifact:
    path: Path
    filename: str
    minecraft: str
    loader: str
    version_number: str
    sha512: str
    sha256: str
    size: int


def _require(condition, message):
    if not condition:
        raise PublishError(message)


def _validate_release(release):
    _require(isinstance(release, dict), "GitHub release JSON must be an object")
    _require(release.get("tag_name") == TAG, "Unexpected GitHub release tag")
    _require(release.get("draft") is False, "GitHub release must not be a draft")
    _require(release.get("prerelease") is False, "This publisher requires a stable GitHub release")
    _require(release.get("html_url") == REPOSITORY + "/releases/tag/" + TAG,
             "GitHub release belongs to an unexpected repository")
    _require(isinstance(release.get("assets"), list), "GitHub release assets are missing")
    _require(release.get("body") is None or isinstance(release.get("body"), str),
             "GitHub release notes must be text")


def load_release(path) -> dict:
    try:
        release = json.loads(Path(path).read_text(encoding="utf-8"))
    except (OSError, ValueError):
        raise PublishError("Cannot read valid GitHub release JSON") from None
    _validate_release(release)
    return release


def _hash_file(path):
    sha512, sha256 = hashlib.sha512(), hashlib.sha256()
    size = 0
    try:
        with path.open("rb") as source:
            while chunk := source.read(1024 * 1024):
                sha512.update(chunk)
                sha256.update(chunk)
                size += len(chunk)
    except OSError:
        raise PublishError("Cannot read release artifact: " + path.name) from None
    return sha512.hexdigest(), sha256.hexdigest(), size


def discover_artifacts(release: dict, jars_dir: Path) -> list[Artifact]:
    _validate_release(release)
    expected = {
        f"JCM-{loader}-{VERSION}+{minecraft}.jar": (minecraft, loader)
        for minecraft in MINECRAFT_VERSIONS for loader in LOADERS
    }
    assets = {}
    for asset in release["assets"]:
        _require(isinstance(asset, dict) and isinstance(asset.get("name"), str),
                 "Malformed GitHub release asset")
        name = asset["name"]
        if not name.lower().endswith(".jar"):
            continue
        _require(name not in assets, "Duplicate GitHub release jar: " + name)
        assets[name] = asset
    _require(set(assets) == set(expected), "GitHub release must contain exactly the twelve expected jars")
    directory = Path(jars_dir)
    _require(directory.is_dir(), "Jar directory does not exist")
    try:
        paths = {path.name: path for path in directory.iterdir()}
    except OSError:
        raise PublishError("Cannot list jar directory") from None
    _require(set(paths) == set(expected), "Jar directory must contain exactly the twelve expected jar files")
    artifacts = []
    for name in sorted(expected):
        path, asset = paths[name], assets[name]
        _require(asset.get("state") == "uploaded", "GitHub release artifact is not fully uploaded: " + name)
        _require(path.is_file() and not path.is_symlink(), "Artifact must be a regular, non-symlink file: " + name)
        sha512, sha256, size = _hash_file(path)
        _require(type(asset.get("size")) is int and asset["size"] == size and size > 0,
                 "GitHub release size mismatch: " + name)
        _require(asset.get("digest") == "sha256:" + sha256,
                 "GitHub release SHA256 is missing or mismatched: " + name)
        minecraft, loader = expected[name]
        artifacts.append(Artifact(path, name, minecraft, loader,
                                  f"{VERSION}+{minecraft}-{loader}", sha512, sha256, size))
    return artifacts


def build_payload(release, artifact, project_id, dependency_ids):
    _validate_release(release)
    _require(set(dependency_ids) == set(DEPENDENCIES), "All dependency identities must be resolved")
    dependencies = [{"project_id": dependency_ids["mtr"], "dependency_type": "required"}]
    if artifact.loader == "fabric":
        dependencies.append({"project_id": dependency_ids["fabric_api"], "dependency_type": "required"})
    dependencies.append({"project_id": dependency_ids["jcm"], "dependency_type": "incompatible"})
    return {
        "name": f"JCM {VERSION} ({artifact.loader.capitalize()} {artifact.minecraft})",
        "version_number": artifact.version_number,
        "changelog": release.get("body") or "",
        "version_type": "release",
        "status": "listed",
        "game_versions": [artifact.minecraft],
        "loaders": [artifact.loader],
        "featured": artifact.loader == "fabric" and artifact.minecraft == "1.20.4",
        "dependencies": dependencies,
        "project_id": project_id,
        "file_parts": [artifact.filename],
        "primary_file": artifact.filename,
    }


def _dependency_signature(dependencies):
    _require(isinstance(dependencies, list), "Malformed version dependencies")
    result = []
    for dependency in dependencies:
        _require(isinstance(dependency, dict), "Malformed version dependency")
        _require(all(dependency.get(key) is None or isinstance(dependency[key], str) for key in
                     ("project_id", "version_id", "file_name", "dependency_type")),
                 "Malformed version dependency fields")
        result.append(tuple(dependency.get(key) or "" for key in
                            ("project_id", "version_id", "file_name", "dependency_type")))
    try:
        return sorted(result)
    except TypeError:
        raise PublishError("Malformed version dependency fields") from None


def matching_version(existing, artifact, payload):
    """Match a complete immutable publication, failing closed on collisions."""
    _require(isinstance(existing, list), "Modrinth versions response must be a list")
    candidates = []
    for version in existing:
        _require(isinstance(version, dict) and isinstance(version.get("files"), list),
                 "Malformed Modrinth version response")
        collision = version.get("version_number") == artifact.version_number
        for file in version["files"]:
            _require(isinstance(file, dict) and isinstance(file.get("hashes"), dict),
                     "Malformed Modrinth version file")
            _require(isinstance(file["hashes"].get("sha512"), str), "Modrinth file SHA512 is missing")
            collision |= (file.get("filename") == artifact.filename or
                          file["hashes"].get("sha512", "").lower() == artifact.sha512)
        if collision:
            candidates.append(version)
    _require(len(candidates) <= 1, "Multiple remote versions collide with " + artifact.filename)
    if not candidates:
        return None
    version = candidates[0]
    prefix = "Remote metadata/file conflict for " + artifact.filename + ": "
    for key in ("project_id", "name", "version_number", "version_type", "status", "featured"):
        _require(version.get(key) == payload[key], prefix + key)
    _require((version.get("changelog") or "") == payload["changelog"], prefix + "changelog")
    for key in ("loaders", "game_versions"):
        _require(isinstance(version.get(key), list) and all(isinstance(value, str) for value in version[key])
                 and sorted(version[key]) == sorted(payload[key]),
                 prefix + key)
    _require(_dependency_signature(version.get("dependencies")) == _dependency_signature(payload["dependencies"]),
             prefix + "dependencies")
    _require(len(version["files"]) == 1, prefix + "expected one primary jar")
    file = version["files"][0]
    _require(file.get("filename") == artifact.filename and type(file.get("size")) is int
             and file["size"] == artifact.size
             and file.get("primary") is True
             and file["hashes"].get("sha512", "").lower() == artifact.sha512,
             prefix + "filename/SHA512/size/primary")
    _require(isinstance(version.get("id"), str) and re.fullmatch(r"[A-Za-z0-9]+", version["id"]),
             prefix + "version id")
    return version


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # Never forward Authorization to a redirect destination.
        return None


class ModrinthClient:
    def __init__(self, token=None, opener=None):
        self._token = os.environ.get("MODRINTH_TOKEN", "") if token is None else token
        self.project_metadata = {}
        self.outcomes = []
        default = urllib.request.build_opener(_NoRedirect()).open
        self._open = getattr(opener, "open", opener) if opener is not None else default

    def _request(self, method, path, data=None, content_type=None):
        headers = {"Accept": "application/json", "User-Agent": USER_AGENT}
        if self._token:
            headers["Authorization"] = self._token
        if content_type:
            headers["Content-Type"] = content_type
        request = urllib.request.Request(API + path, data=data, headers=headers, method=method)
        error_type = UncertainUpload if method == "POST" else PublishError
        try:
            with self._open(request, timeout=600 if method == "POST" else 60) as response:
                if not 200 <= response.status < 300:
                    raise error_type(f"Modrinth {method} returned HTTP {response.status}")
                return json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as error:
            # Do not print response bodies, request objects, or headers.
            raise error_type(f"Modrinth {method} returned HTTP {error.code}") from None
        except (urllib.error.URLError, OSError, http.client.HTTPException, ValueError):
            raise error_type(f"Modrinth {method} transport/response failure") from None

    def get_project(self, identifier):
        _require(isinstance(identifier, str) and re.fullmatch(r"[A-Za-z0-9_-]+", identifier), "Invalid project identifier")
        project = self._request("GET", "/project/" + identifier)
        if isinstance(project, dict):
            self.project_metadata[identifier] = {
                key: project.get(key) if isinstance(project.get(key), str) else None
                for key in ("id", "title", "slug", "status", "requested_status", "source_url", "project_type")
            }
        return project

    def list_versions(self, project_id):
        _require(isinstance(project_id, str) and re.fullmatch(r"[A-Za-z0-9]+", project_id), "Invalid project ID")
        result, seen = [], set()
        for page in range(1000):
            query = urllib.parse.urlencode({"limit": 100, "offset": page * 100})
            versions = self._request("GET", f"/project/{project_id}/version?{query}")
            _require(isinstance(versions, list) and len(versions) <= 100, "Invalid Modrinth version page")
            for version in versions:
                _require(isinstance(version, dict) and isinstance(version.get("id"), str), "Malformed Modrinth version")
                _require(version["id"] not in seen, "Repeated Modrinth pagination entry")
                seen.add(version["id"])
                result.append(version)
            if len(versions) < 100:
                return result
        raise PublishError("Modrinth version pagination limit exceeded")

    def create_version(self, payload, artifact):
        _require(bool(self._token), "MODRINTH_TOKEN is required for publication")
        _require(artifact.path.is_file() and not artifact.path.is_symlink(), "Artifact changed after preflight")
        try:
            blob = artifact.path.read_bytes()
        except OSError:
            raise PublishError("Cannot read artifact for upload") from None
        _require(len(blob) == artifact.size and hashlib.sha512(blob).hexdigest() == artifact.sha512
                 and hashlib.sha256(blob).hexdigest() == artifact.sha256, "Artifact changed after preflight")
        boundary = "----jcm-modrinth-" + uuid.uuid4().hex
        body = (f'--{boundary}\r\nContent-Disposition: form-data; name="data"\r\n'
                'Content-Type: application/json\r\n\r\n').encode()
        body += json.dumps(payload, ensure_ascii=False).encode("utf-8")
        body += (f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="{artifact.filename}"; '
                 f'filename="{artifact.filename}"\r\nContent-Type: application/java-archive\r\n\r\n').encode()
        body += blob + f"\r\n--{boundary}--\r\n".encode()
        result = self._request("POST", "/version", body, "multipart/form-data; boundary=" + boundary)
        if not isinstance(result, dict):
            raise UncertainUpload("Modrinth POST returned an unexpected response")
        return result


def _project_identity(project, expected_id):
    _require(isinstance(project, dict) and project.get("id") == expected_id
             and project.get("project_type") == "mod", "Modrinth project identity/type mismatch")


def _reconcile(client, project_id, artifact, payload, sleep):
    for delay in (0, 3, 10):
        if delay:
            sleep(delay)
        try:
            versions = client.list_versions(project_id)
        except PublishError:
            continue
        match = matching_version(versions, artifact, payload)
        if match is not None:
            return match
    raise PublishError("Upload outcome unresolved for " + artifact.filename
                       + "; no POST was retried. Re-run the read-only preflight before any further publication.")


def publish_release(release, jars_dir, project_id, client, dry_run=False, sleep=time.sleep):
    artifacts = discover_artifacts(release, jars_dir)
    _require(isinstance(project_id, str) and re.fullmatch(r"[A-Za-z0-9]+", project_id), "Invalid target project ID")
    project = client.get_project(project_id)
    if isinstance(project, dict):
        public_project = {
            key: project.get(key) if isinstance(project.get(key), str) else None
            for key in ("id", "title", "slug", "status", "requested_status", "source_url", "project_type")
        }
        print(json.dumps({"project": public_project}, ensure_ascii=False), flush=True)
    _project_identity(project, project_id)
    _require(project.get("source_url") is None or isinstance(project.get("source_url"), str),
             "Malformed Modrinth target source URL")
    source_url = (project.get("source_url") or "").rstrip("/")
    if source_url.endswith(".git"):
        source_url = source_url[:-4]
    _require(source_url.lower() == REPOSITORY.lower(), "Modrinth target source URL does not match this repository")
    dependency_ids = {}
    for key, (slug, expected_id) in DEPENDENCIES.items():
        dependency = client.get_project(slug)
        _project_identity(dependency, expected_id)
        _require(dependency.get("slug") == slug, "Modrinth dependency slug mismatch")
        _require(expected_id != project_id, "Target project cannot also be a dependency")
        dependency_ids[key] = expected_id
    existing = client.list_versions(project_id)
    plan = []
    # Resolve every collision before the first possible write, including a
    # conflict in the last jar of the batch.
    for artifact in artifacts:
        payload = build_payload(release, artifact, project_id, dependency_ids)
        plan.append((artifact, payload, matching_version(existing, artifact, payload)))
    results = []
    if isinstance(client, ModrinthClient):
        client.outcomes = results
    for artifact, payload, match in plan:
        action = "skip" if match is not None else "would_upload"
        if not dry_run:
            # Re-read immediately before writing so a concurrent completed
            # upload is detected. The workflow must also serialize writers.
            match = matching_version(client.list_versions(project_id), artifact, payload)
            if match is not None:
                action = "skip"
            else:
                try:
                    response = client.create_version(payload, artifact)
                except UncertainUpload:
                    match = _reconcile(client, project_id, artifact, payload, sleep)
                    action = "reconciled"
                else:
                    match = matching_version([response], artifact, payload)
                    if match is None:
                        match = _reconcile(client, project_id, artifact, payload, sleep)
                    action = "uploaded"
        result = {"action": action, "filename": artifact.filename,
                  "version_number": artifact.version_number,
                  "version_id": match["id"] if match is not None else None}
        print(json.dumps(result, ensure_ascii=False), flush=True)
        results.append(result)
    if not dry_run:
        final_versions = client.list_versions(project_id)
        for artifact, payload, _ in plan:
            if matching_version(final_versions, artifact, payload) is None:
                _reconcile(client, project_id, artifact, payload, sleep)
        print(json.dumps({"verified_remote_versions": len(plan)}), flush=True)
    return results


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--release-json", required=True, type=Path)
    parser.add_argument("--jars-dir", required=True, type=Path)
    parser.add_argument("--project-id", required=True)
    parser.add_argument("--dry-run", action="store_true", help="Read-only remote preflight; never POST")
    parser.add_argument("--output-json", type=Path, help="Write public project metadata and publication outcomes")
    args = parser.parse_args(argv)
    client = ModrinthClient()
    code, error_message = 0, None
    try:
        _require(bool(os.environ.get("MODRINTH_TOKEN")), "MODRINTH_TOKEN is required for authenticated preflight/publication")
        publish_release(load_release(args.release_json), args.jars_dir,
                        args.project_id, client, dry_run=args.dry_run)
    except PublishError as error:
        code, error_message = 1, str(error)
        print("Publication stopped: " + error_message, file=sys.stderr)
    if args.output_json:
        summary = {"project": client.project_metadata.get(args.project_id), "version": VERSION,
                   "dry_run": args.dry_run, "success": code == 0,
                   "artifacts": client.outcomes, "error": error_message}
        try:
            args.output_json.parent.mkdir(parents=True, exist_ok=True)
            args.output_json.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        except OSError:
            print("Cannot write publication summary", file=sys.stderr)
            return 1
    return code


if __name__ == "__main__":
    sys.exit(main())
