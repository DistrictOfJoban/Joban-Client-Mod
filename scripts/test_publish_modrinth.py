#!/usr/bin/env python3
"""Offline publisher regressions, intended to run only in GitHub Actions.

All artifacts are temporary fixtures and every HTTP call uses a fake transport.
No test imports credentials, contacts Modrinth, or invokes a build tool.
"""
from __future__ import annotations

from contextlib import redirect_stderr, redirect_stdout
from copy import deepcopy
from email import policy
from email.parser import BytesParser
import hashlib
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest import mock
import urllib.error
import urllib.parse

sys.path.insert(0, str(Path(__file__).resolve().parent))
import publish_modrinth as publisher


VERSION = "2.3.0-1.0.0"
PROJECT = "kMMJcRqY"
REPOSITORY = "https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang"
MINECRAFT = ("1.17.1", "1.18.2", "1.19.2", "1.19.4", "1.20.1", "1.20.4")
DEPENDENCIES = {"mtr": "XKPAmI6u", "fabric_api": "P7dR8mSH", "jcm": "lQqKCDhg"}


def remote_version(artifact, payload, identifier="Version1"):
    result = deepcopy(payload)
    result.pop("file_parts")
    result.pop("primary_file")
    result.update({
        "id": identifier,
        "files": [{"filename": artifact.filename, "size": artifact.size,
                   "primary": True, "hashes": {"sha512": artifact.sha512}}],
    })
    return result


class FakeClient:
    """In-memory Modrinth; ambiguous uploads can commit before losing a reply."""

    def __init__(self, existing=()):
        self.projects = {
            PROJECT: {"id": PROJECT, "slug": "jcm-yanyang", "project_type": "mod",
                      "source_url": REPOSITORY},
            "minecraft-transit-railway": {"id": DEPENDENCIES["mtr"],
                "slug": "minecraft-transit-railway", "project_type": "mod"},
            "fabric-api": {"id": DEPENDENCIES["fabric_api"],
                "slug": "fabric-api", "project_type": "mod"},
            "jcm": {"id": DEPENDENCIES["jcm"], "slug": "jcm", "project_type": "mod"},
        }
        self.versions = deepcopy(list(existing))
        self.project_reads = []
        self.version_reads = 0
        self.posts = []
        self.first_upload = "normal"
        self.pending = None
        self.visibility_reads = 0
        self.concurrent_version = None

    def get_project(self, identifier):
        self.project_reads.append(identifier)
        return deepcopy(self.projects[identifier])

    def list_versions(self, project_id):
        if project_id != PROJECT:
            raise AssertionError("Unexpected target project")
        self.version_reads += 1
        if self.version_reads == 2 and self.concurrent_version is not None:
            self.versions.append(deepcopy(self.concurrent_version))
        if self.pending is not None:
            self.visibility_reads -= 1
            if self.visibility_reads == 0:
                self.versions.append(self.pending)
                self.pending = None
        return deepcopy(self.versions)

    def create_version(self, payload, artifact):
        self.posts.append((artifact.filename, deepcopy(payload)))
        result = remote_version(artifact, payload, "Version" + str(len(self.posts)))
        if len(self.posts) == 1 and self.first_upload == "unconfirmed":
            raise publisher.UncertainUpload("Simulated lost response without visible commit")
        if len(self.posts) == 1 and self.first_upload == "delayed":
            self.pending = result
            self.visibility_reads = 2
            raise publisher.UncertainUpload("Simulated delayed visibility")
        self.versions.append(deepcopy(result))
        if len(self.posts) == 1 and self.first_upload == "committed":
            raise publisher.UncertainUpload("Simulated committed POST with lost response")
        if len(self.posts) == 1 and self.first_upload == "bad_metadata":
            result["version_type"] = "beta"
        return result


class Response(io.BytesIO):
    def __init__(self, value, status=200):
        body = value if isinstance(value, bytes) else json.dumps(value).encode("utf-8")
        super().__init__(body)
        self.status = status


class RecordingOpener:
    def __init__(self, responses):
        self.responses = list(responses)
        self.requests = []

    def __call__(self, request, timeout):
        self.requests.append((request, timeout))
        if not self.responses:
            raise AssertionError("Unexpected additional HTTP request")
        response = self.responses.pop(0)
        if isinstance(response, BaseException):
            raise response
        return Response(response)


class ReleaseFixture(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.jars = self.root / "jars"
        self.jars.mkdir()
        self.release = {
            "tag_name": "v" + VERSION,
            "draft": False,
            "prerelease": False,
            "html_url": REPOSITORY + "/releases/tag/v" + VERSION,
            "body": "正式版说明：屏蔽门、线路数据与原生脚本修复。\n",
            "assets": [],
        }
        for minecraft in MINECRAFT:
            for loader in ("fabric", "forge"):
                name = f"JCM-{loader}-{VERSION}+{minecraft}.jar"
                content = b"PK\x03\x04" + name.encode("ascii") + b"\nfixture-only\n"
                (self.jars / name).write_bytes(content)
                self.release["assets"].append({
                    "name": name, "size": len(content), "state": "uploaded",
                    "digest": "sha256:" + hashlib.sha256(content).hexdigest(),
                })
        self.release["assets"].append({"name": "jcm-native-sdk-" + VERSION + ".zip", "size": 1})
        self.delays = []

    def artifacts(self):
        return publisher.discover_artifacts(self.release, self.jars)

    def payload(self, artifact):
        return publisher.build_payload(self.release, artifact, PROJECT, DEPENDENCIES)

    def existing_versions(self):
        return [remote_version(artifact, self.payload(artifact), "Existing" + str(index))
                for index, artifact in enumerate(self.artifacts())]

    def publish(self, client, dry_run=False):
        with redirect_stdout(io.StringIO()):
            return publisher.publish_release(self.release, self.jars, PROJECT, client,
                                             dry_run=dry_run, sleep=self.delays.append)


class ReleaseValidationTests(ReleaseFixture):
    def test_loads_only_the_full_stable_release(self):
        path = self.root / "release.json"
        path.write_text(json.dumps(self.release), encoding="utf-8")
        self.assertEqual(publisher.load_release(path), self.release)
        invalid = [
            ("tag_name", "v2.3.0"), ("tag_name", "v2.3.0-1.0.0-build.1"),
            ("tag_name", "2.3.0-1.0.0"), ("draft", True), ("draft", None),
            ("prerelease", True), ("prerelease", None),
            ("html_url", "https://github.com/elsewhere/repo/releases/tag/v" + VERSION),
            ("assets", None), ("body", {"unexpected": "object"}),
        ]
        for key, value in invalid:
            with self.subTest(field=key, value=value):
                release = deepcopy(self.release)
                release[key] = value
                path.write_text(json.dumps(release), encoding="utf-8")
                with self.assertRaises(publisher.PublishError):
                    publisher.load_release(path)

    def test_invalid_json_missing_file_and_non_object_fail_closed(self):
        path = self.root / "invalid.json"
        for content in ("{broken", "[]", "null"):
            with self.subTest(content=content):
                path.write_text(content, encoding="utf-8")
                with self.assertRaises(publisher.PublishError):
                    publisher.load_release(path)
        with self.assertRaises(publisher.PublishError):
            publisher.load_release(self.root / "missing.json")

    def test_discovers_exact_six_by_two_matrix_with_full_hashes(self):
        artifacts = self.artifacts()
        self.assertEqual(len(artifacts), 12)
        self.assertEqual({(a.minecraft, a.loader) for a in artifacts},
                         {(m, loader) for m in MINECRAFT for loader in ("fabric", "forge")})
        self.assertEqual(len({a.version_number for a in artifacts}), 12)
        for artifact in artifacts:
            with self.subTest(filename=artifact.filename):
                blob = artifact.path.read_bytes()
                self.assertEqual(artifact.filename, f"JCM-{artifact.loader}-{VERSION}+{artifact.minecraft}.jar")
                self.assertEqual(artifact.version_number, f"{VERSION}+{artifact.minecraft}-{artifact.loader}")
                self.assertEqual(artifact.sha512, hashlib.sha512(blob).hexdigest())
                self.assertEqual(artifact.sha256, hashlib.sha256(blob).hexdigest())
                self.assertEqual(artifact.size, len(blob))

    def test_missing_extra_duplicate_or_wrongly_named_release_jars_rejected(self):
        original = deepcopy(self.release)
        variants = []
        missing = deepcopy(original)
        missing["assets"].pop(0)
        variants.append(missing)
        duplicate = deepcopy(original)
        duplicate["assets"].append(deepcopy(duplicate["assets"][0]))
        variants.append(duplicate)
        for name in ("JCM-fabric-2.3.0+1.20.4.jar", "JCM-forge-2.3.0-1.0.0+1.21.1.jar", "extra.jar"):
            changed = deepcopy(original)
            changed["assets"][0]["name"] = name
            variants.append(changed)
        extra = deepcopy(original)
        extra["assets"].append({"name": "extra.jar", "size": 1, "digest": "sha256:" + "0" * 64})
        variants.append(extra)
        for index, release in enumerate(variants):
            with self.subTest(variant=index), self.assertRaises(publisher.PublishError):
                publisher.discover_artifacts(release, self.jars)

    def test_missing_extra_and_wrongly_named_local_files_rejected(self):
        source = self.artifacts()[0].path
        saved = source.read_bytes()
        source.unlink()
        with self.assertRaises(publisher.PublishError):
            self.artifacts()
        source.write_bytes(saved)
        extra = self.jars / "README.txt"
        extra.write_text("Unexpected file", encoding="utf-8")
        with self.assertRaises(publisher.PublishError):
            self.artifacts()
        extra.unlink()
        source.rename(source.with_name("wrong-version.jar"))
        with self.assertRaises(publisher.PublishError):
            self.artifacts()

    def test_symlink_and_directory_cannot_stand_in_for_release_jar(self):
        path = self.artifacts()[0].path
        outside = self.root / "outside.jar"
        outside.write_bytes(path.read_bytes())
        path.unlink()
        path.symlink_to(outside)
        with self.assertRaises(publisher.PublishError):
            self.artifacts()
        path.unlink()
        path.mkdir()
        with self.assertRaises(publisher.PublishError):
            self.artifacts()

    def test_release_size_and_digest_must_match(self):
        for field, value in (("size", 0), ("size", True), ("size", 999999),
                             ("digest", None), ("digest", "sha256:" + "0" * 64),
                             ("digest", "sha512:" + "0" * 128)):
            release = deepcopy(self.release)
            release["assets"][0][field] = value
            with self.subTest(field=field, value=value), self.assertRaises(publisher.PublishError):
                publisher.discover_artifacts(release, self.jars)

    def test_incomplete_upload_state_is_rejected(self):
        for state in (None, "new", "open", "failed"):
            release = deepcopy(self.release)
            release["assets"][0]["state"] = state
            with self.subTest(state=state), self.assertRaises(publisher.PublishError):
                publisher.discover_artifacts(release, self.jars)

    def test_same_size_file_tampering_is_detected(self):
        path = self.artifacts()[0].path
        content = path.read_bytes()
        path.write_bytes(content[:-1] + b"X")
        with self.assertRaises(publisher.PublishError):
            self.artifacts()


class PayloadTests(ReleaseFixture):
    def test_every_payload_keeps_full_version_and_stable_channel(self):
        featured = []
        for artifact in self.artifacts():
            with self.subTest(filename=artifact.filename):
                payload = self.payload(artifact)
                self.assertEqual(payload["project_id"], PROJECT)
                self.assertEqual(payload["version_number"], f"{VERSION}+{artifact.minecraft}-{artifact.loader}")
                self.assertEqual(payload["name"], f"JCM {VERSION} ({artifact.loader.capitalize()} {artifact.minecraft})")
                self.assertEqual(payload["version_type"], "release")
                self.assertEqual(payload["status"], "listed")
                self.assertEqual(payload["game_versions"], [artifact.minecraft])
                self.assertEqual(payload["loaders"], [artifact.loader])
                self.assertEqual(payload["changelog"], self.release["body"])
                self.assertEqual(payload["file_parts"], [artifact.filename])
                self.assertEqual(payload["primary_file"], artifact.filename)
                expected = {(DEPENDENCIES["mtr"], "required"), (DEPENDENCIES["jcm"], "incompatible")}
                if artifact.loader == "fabric":
                    expected.add((DEPENDENCIES["fabric_api"], "required"))
                self.assertEqual({(d["project_id"], d["dependency_type"]) for d in payload["dependencies"]}, expected)
                self.assertEqual(len(payload["dependencies"]), len(expected))
                if payload["featured"]:
                    featured.append((artifact.minecraft, artifact.loader))
        self.assertEqual(featured, [("1.20.4", "fabric")])

    def test_missing_dependency_resolution_is_rejected(self):
        artifact = self.artifacts()[0]
        with self.assertRaises(publisher.PublishError):
            publisher.build_payload(self.release, artifact, PROJECT, {"mtr": DEPENDENCIES["mtr"]})

    def test_empty_release_notes_remain_empty(self):
        self.release["body"] = None
        self.assertEqual(self.payload(self.artifacts()[0])["changelog"], "")


class MatchingTests(ReleaseFixture):
    def setUp(self):
        super().setUp()
        self.artifact = self.artifacts()[0]
        self.expected = self.payload(self.artifact)
        self.remote = remote_version(self.artifact, self.expected)

    def test_exact_match_is_returned_and_dependency_order_is_irrelevant(self):
        self.remote["dependencies"].reverse()
        self.remote["files"][0]["hashes"]["sha512"] = self.artifact.sha512.upper()
        self.assertIs(publisher.matching_version([self.remote], self.artifact, self.expected), self.remote)

    def test_unrelated_version_does_not_match(self):
        self.remote["version_number"] = "an-older-version"
        self.remote["files"][0]["filename"] = "old.jar"
        self.remote["files"][0]["hashes"]["sha512"] = "0" * 128
        self.assertIsNone(publisher.matching_version([self.remote], self.artifact, self.expected))

    def test_metadata_conflicts_are_not_treated_as_safe_duplicates(self):
        changes = {"project_id": "OtherProject", "name": "JCM 2.3.0",
                   "version_number": "2.3.0", "version_type": "beta", "status": "unlisted",
                   "featured": not self.expected["featured"], "changelog": "different",
                   "loaders": ["fabric", "forge"], "game_versions": ["1.20.4", "1.20.1"],
                   "dependencies": [], "id": "bad/id"}
        for field, value in changes.items():
            changed = deepcopy(self.remote)
            changed[field] = value
            with self.subTest(field=field), self.assertRaises(publisher.PublishError):
                publisher.matching_version([changed], self.artifact, self.expected)

    def test_file_hash_size_name_primary_and_count_conflicts_rejected(self):
        changes = (("filename", "different.jar"), ("size", self.artifact.size + 1),
                   ("primary", False), ("hashes", {"sha512": "0" * 128}))
        for field, value in changes:
            changed = deepcopy(self.remote)
            changed["files"][0][field] = value
            with self.subTest(field=field), self.assertRaises(publisher.PublishError):
                publisher.matching_version([changed], self.artifact, self.expected)
        changed = deepcopy(self.remote)
        changed["files"].append(deepcopy(changed["files"][0]))
        with self.assertRaises(publisher.PublishError):
            publisher.matching_version([changed], self.artifact, self.expected)

    def test_filename_or_hash_collision_with_other_version_is_rejected(self):
        for keep in ("filename", "hash"):
            changed = deepcopy(self.remote)
            changed["version_number"] = "different-version"
            if keep == "filename":
                changed["files"][0]["hashes"]["sha512"] = "0" * 128
            else:
                changed["files"][0]["filename"] = "other.jar"
            with self.subTest(collision=keep), self.assertRaises(publisher.PublishError):
                publisher.matching_version([changed], self.artifact, self.expected)

    def test_multiple_colliding_versions_are_rejected(self):
        duplicate = deepcopy(self.remote)
        duplicate["id"] = "Version2"
        with self.assertRaises(publisher.PublishError):
            publisher.matching_version([self.remote, duplicate], self.artifact, self.expected)

    def test_malformed_versions_fail_closed(self):
        invalid = (None, {}, [None], [{"files": None}], [{"files": [{}]}])
        for versions in invalid:
            with self.subTest(versions=versions), self.assertRaises(publisher.PublishError):
                publisher.matching_version(versions, self.artifact, self.expected)


class PublicationTests(ReleaseFixture):
    def test_complete_release_uploads_twelve_unique_versions(self):
        client = FakeClient()
        results = self.publish(client)
        self.assertEqual(len(results), 12)
        self.assertEqual({r["action"] for r in results}, {"uploaded"})
        self.assertEqual(len(client.posts), 12)
        self.assertEqual(len({name for name, payload in client.posts}), 12)
        self.assertEqual(set(client.project_reads), {PROJECT, "minecraft-transit-railway", "fabric-api", "jcm"})
        self.assertEqual(self.delays, [])

    def test_repeated_run_skips_all_exact_publications(self):
        client = FakeClient(self.existing_versions())
        results = self.publish(client)
        self.assertEqual(len(results), 12)
        self.assertEqual({r["action"] for r in results}, {"skip"})
        self.assertEqual(client.posts, [])

    def test_partial_publication_uploads_only_missing_artifacts(self):
        client = FakeClient(self.existing_versions()[:6])
        results = self.publish(client)
        self.assertEqual(sum(r["action"] == "skip" for r in results), 6)
        self.assertEqual(sum(r["action"] == "uploaded" for r in results), 6)
        self.assertEqual(len(client.posts), 6)

    def test_last_artifact_conflict_aborts_before_any_post(self):
        conflicting = self.existing_versions()[-1]
        conflicting["version_type"] = "beta"
        client = FakeClient([conflicting])
        with self.assertRaises(publisher.PublishError):
            self.publish(client)
        self.assertEqual(client.posts, [])

    def test_dry_run_never_posts_and_reports_missing_and_existing(self):
        client = FakeClient(self.existing_versions()[:1])
        results = self.publish(client, dry_run=True)
        self.assertEqual(len(results), 12)
        self.assertEqual(sum(r["action"] == "skip" for r in results), 1)
        self.assertEqual(sum(r["action"] == "would_upload" for r in results), 11)
        self.assertEqual(client.posts, [])

    def test_project_identity_type_and_source_url_errors_abort_before_post(self):
        for field, value in (("id", "WrongProject"), ("project_type", "modpack"),
                             ("source_url", "https://github.com/other/repo"), ("source_url", None)):
            client = FakeClient()
            client.projects[PROJECT][field] = value
            with self.subTest(field=field), self.assertRaises(publisher.PublishError):
                self.publish(client)
            self.assertEqual(client.posts, [])

    def test_dependency_id_type_and_slug_are_verified(self):
        for slug in ("minecraft-transit-railway", "fabric-api", "jcm"):
            for field, value in (("id", "WrongId"), ("project_type", "modpack"), ("slug", "wrong-slug")):
                client = FakeClient()
                client.projects[slug][field] = value
                with self.subTest(slug=slug, field=field), self.assertRaises(publisher.PublishError):
                    self.publish(client)
                self.assertEqual(client.posts, [])

    def test_failed_authentication_stops_before_any_post(self):
        client = FakeClient()
        with mock.patch.object(client, "get_project", side_effect=publisher.PublishError("HTTP 401")):
            with self.assertRaises(publisher.PublishError):
                self.publish(client)
        self.assertEqual(client.posts, [])

    def test_committed_post_with_lost_response_is_not_reposted(self):
        client = FakeClient()
        client.first_upload = "committed"
        results = self.publish(client)
        self.assertEqual(results[0]["action"], "reconciled")
        self.assertEqual(len(client.posts), 12)
        self.assertEqual(len({name for name, payload in client.posts}), 12)
        self.assertEqual(len(client.versions), 12)

    def test_delayed_visibility_retries_only_get(self):
        client = FakeClient()
        client.first_upload = "delayed"
        results = self.publish(client)
        self.assertEqual(results[0]["action"], "reconciled")
        self.assertEqual(self.delays, [3])
        self.assertEqual(len(client.posts), 12)
        self.assertEqual(len({name for name, payload in client.posts}), 12)

    def test_unconfirmed_upload_stops_batch_without_retrying_post(self):
        client = FakeClient()
        client.first_upload = "unconfirmed"
        with self.assertRaisesRegex(publisher.PublishError, "unresolved"):
            self.publish(client)
        self.assertEqual(len(client.posts), 1)
        self.assertEqual(client.version_reads, 5)  # preflight + pre-POST + three read-only reconciliations
        self.assertEqual(self.delays, [3, 10])

    def test_success_response_with_conflicting_metadata_stops_batch(self):
        client = FakeClient()
        client.first_upload = "bad_metadata"
        with self.assertRaises(publisher.PublishError):
            self.publish(client)
        self.assertEqual(len(client.posts), 1)

    def test_concurrent_completed_upload_is_seen_before_post(self):
        client = FakeClient()
        client.concurrent_version = self.existing_versions()[0]
        results = self.publish(client)
        self.assertEqual(results[0]["action"], "skip")
        self.assertEqual(len(client.posts), 11)
        self.assertNotIn(results[0]["filename"], [name for name, payload in client.posts])


class TransportTests(ReleaseFixture):
    def setUp(self):
        super().setUp()
        self.artifact = self.artifacts()[0]
        self.expected = self.payload(self.artifact)

    def test_missing_token_prevents_even_one_post(self):
        opener = RecordingOpener([])
        client = publisher.ModrinthClient(token="", opener=opener)
        with self.assertRaisesRegex(publisher.PublishError, "MODRINTH_TOKEN"):
            client.create_version(self.expected, self.artifact)
        self.assertEqual(opener.requests, [])

    def test_http_authentication_errors_are_sanitized_and_not_retried(self):
        secret = "fixture-token-must-not-appear-in-errors"
        for code in (401, 403):
            error = urllib.error.HTTPError("https://api.modrinth.com/v2/project/" + PROJECT,
                                           code, secret, {}, io.BytesIO(secret.encode()))
            opener = RecordingOpener([error])
            client = publisher.ModrinthClient(token=secret, opener=opener)
            with self.subTest(code=code), self.assertRaises(publisher.PublishError) as caught:
                client.get_project(PROJECT)
            self.assertNotIn(secret, str(caught.exception))
            self.assertEqual(len(opener.requests), 1)
            self.assertEqual(opener.requests[0][0].get_method(), "GET")

    def test_post_transport_failure_is_uncertain_and_never_retried(self):
        for response in (urllib.error.URLError("lost reply"), b"not-json", []):
            opener = RecordingOpener([response])
            client = publisher.ModrinthClient(token="fixture-token", opener=opener)
            with self.subTest(response=type(response).__name__), self.assertRaises(publisher.UncertainUpload):
                client.create_version(self.expected, self.artifact)
            self.assertEqual(len(opener.requests), 1)
            self.assertEqual(opener.requests[0][0].get_method(), "POST")

    def test_jar_changed_after_preflight_cannot_be_uploaded(self):
        content = self.artifact.path.read_bytes()
        self.artifact.path.write_bytes(content[:-1] + b"X")
        opener = RecordingOpener([])
        client = publisher.ModrinthClient(token="fixture-token", opener=opener)
        with self.assertRaises(publisher.PublishError):
            client.create_version(self.expected, self.artifact)
        self.assertEqual(opener.requests, [])

    def test_multipart_contains_exact_metadata_and_one_original_jar(self):
        remote = remote_version(self.artifact, self.expected)
        opener = RecordingOpener([remote])
        client = publisher.ModrinthClient(token="fixture-token", opener=opener)
        self.assertEqual(client.create_version(self.expected, self.artifact), remote)
        request, timeout = opener.requests[0]
        self.assertEqual(request.full_url, "https://api.modrinth.com/v2/version")
        self.assertEqual(request.get_method(), "POST")
        self.assertEqual(request.get_header("Authorization"), "fixture-token")
        self.assertGreater(timeout, 0)
        message = BytesParser(policy=policy.default).parsebytes(
            ("Content-Type: " + request.get_header("Content-type") + "\r\nMIME-Version: 1.0\r\n\r\n").encode()
            + request.data)
        parts = list(message.iter_parts())
        self.assertEqual(len(parts), 2)
        self.assertEqual(parts[0].get_param("name", header="content-disposition"), "data")
        self.assertEqual(json.loads(parts[0].get_payload(decode=True)), self.expected)
        self.assertEqual(parts[1].get_param("name", header="content-disposition"), self.artifact.filename)
        self.assertEqual(parts[1].get_filename(), self.artifact.filename)
        self.assertEqual(parts[1].get_payload(decode=True), self.artifact.path.read_bytes())

    def test_invalid_identifiers_cannot_change_request_path(self):
        opener = RecordingOpener([])
        client = publisher.ModrinthClient(token="", opener=opener)
        for identifier in ("../project", "id?token=bad", "https://other.invalid", ""):
            with self.subTest(identifier=identifier), self.assertRaises(publisher.PublishError):
                client.get_project(identifier)
        self.assertEqual(opener.requests, [])

    def test_all_version_pages_are_read_for_deduplication(self):
        first = [{"id": "v" + str(index)} for index in range(100)]
        last = [{"id": "last"}]
        opener = RecordingOpener([first, last])
        result = publisher.ModrinthClient(token="", opener=opener).list_versions(PROJECT)
        self.assertEqual(result, first + last)
        self.assertEqual(len(opener.requests), 2)
        offsets = [urllib.parse.parse_qs(urllib.parse.urlsplit(request.full_url).query)["offset"]
                   for request, timeout in opener.requests]
        self.assertEqual(offsets, [["0"], ["100"]])

    def test_repeated_pagination_entries_fail_closed(self):
        first = [{"id": "v" + str(index)} for index in range(100)]
        opener = RecordingOpener([first, [{"id": "v0"}]])
        with self.assertRaises(publisher.PublishError):
            publisher.ModrinthClient(token="", opener=opener).list_versions(PROJECT)
        self.assertEqual(len(opener.requests), 2)


class CommandLineTests(ReleaseFixture):
    def arguments(self, dry_run):
        release_json = self.root / "release.json"
        release_json.write_text(json.dumps(self.release), encoding="utf-8")
        args = ["--release-json", str(release_json), "--jars-dir", str(self.jars),
                "--project-id", PROJECT, "--output-json", str(self.root / "summary.json")]
        return args + (["--dry-run"] if dry_run else [])

    def invoke(self, args, opener, environment):
        with mock.patch.dict(publisher.os.environ, environment, clear=True), \
                mock.patch.object(publisher.urllib.request, "build_opener") as builder, \
                redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()):
            builder.return_value.open = opener
            return publisher.main(args)

    def test_missing_token_blocks_publication_and_dry_run_without_http(self):
        for dry_run in (False, True):
            opener = RecordingOpener([])
            with self.subTest(dry_run=dry_run):
                result = self.invoke(self.arguments(dry_run), opener, {})
                self.assertEqual(result, 1)
                self.assertEqual(opener.requests, [])
                summary = json.loads((self.root / "summary.json").read_text(encoding="utf-8"))
                self.assertFalse(summary["success"])
                self.assertEqual(summary["version"], VERSION)
                self.assertEqual(summary["artifacts"], [])
                self.assertIn("MODRINTH_TOKEN", summary["error"])

    def test_authenticated_dry_run_uses_only_get_and_saves_public_summary(self):
        projects = FakeClient().projects
        opener = RecordingOpener([projects[PROJECT], projects["minecraft-transit-railway"],
                                  projects["fabric-api"], projects["jcm"], []])
        token = "fixture-secret-never-written-to-summary"
        result = self.invoke(self.arguments(True), opener, {"MODRINTH_TOKEN": token})
        self.assertEqual(result, 0)
        self.assertEqual(len(opener.requests), 5)
        self.assertEqual({request.get_method() for request, timeout in opener.requests}, {"GET"})
        self.assertTrue(all(request.get_header("Authorization") == token for request, timeout in opener.requests))
        summary_text = (self.root / "summary.json").read_text(encoding="utf-8")
        summary = json.loads(summary_text)
        self.assertNotIn(token, summary_text)
        self.assertTrue(summary["success"])
        self.assertTrue(summary["dry_run"])
        self.assertEqual(summary["version"], VERSION)
        self.assertEqual(summary["project"]["id"], PROJECT)
        self.assertEqual(len(summary["artifacts"]), 12)
        self.assertEqual({artifact["action"] for artifact in summary["artifacts"]}, {"would_upload"})

    def test_authentication_failure_saves_failed_summary_without_post(self):
        token = "fixture-secret-not-in-error-output"
        error = urllib.error.HTTPError("https://api.modrinth.com/v2/project/" + PROJECT,
                                      401, token, {}, io.BytesIO(token.encode()))
        opener = RecordingOpener([error])
        result = self.invoke(self.arguments(True), opener, {"MODRINTH_TOKEN": token})
        self.assertEqual(result, 1)
        self.assertEqual(len(opener.requests), 1)
        self.assertEqual(opener.requests[0][0].get_method(), "GET")
        summary_text = (self.root / "summary.json").read_text(encoding="utf-8")
        self.assertNotIn(token, summary_text)
        self.assertFalse(json.loads(summary_text)["success"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
