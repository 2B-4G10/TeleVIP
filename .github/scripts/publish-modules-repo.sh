#!/usr/bin/env bash
# Mirrors this release to the module's own repository in the Xposed Modules Repo
# (modules.lsposed.org, Xposed-Modules-Repo/<applicationId>):
#   - its description and home page, the SUMMARY from .github/modules-repo/, and this repository's
#     README.md with its relative images and links pointed back here, so they work over there;
#   - a release tagged <versionCode>-<versionName> with the release APK and the changelog,
#     which is what the repository's bot reads.
# Expects artifacts/TeleVip-<versionName>-release.apk and notes.md from release.yml, and GH_TOKEN
# with write access to that repository.
set -euo pipefail

pkg=$(sed -n 's/^ *applicationId = "\(.*\)"$/\1/p' app/build.gradle)
code=$(sed -n 's/^ *versionCode = \([0-9]*\)$/\1/p' app/build.gradle)
name=$(sed -n 's/^ *versionName = "\(.*\)"$/\1/p' app/build.gradle)
repo="Xposed-Modules-Repo/$pkg"
tag="$code-$name"
apk="artifacts/TeleVip-$name-release.apk"

# GitHub answers 404 rather than 403 when a token can see a public repository but not write to it.
if [ "$(gh api "repos/$repo" --jq '.permissions.push // false' 2>/dev/null)" != "true" ]; then
  echo "::error::MODULES_REPO_TOKEN cannot write to $repo. Accept the maintainer invitation at" \
    "https://github.com/$repo/invitations, and use a classic token with the public_repo scope" \
    "(a fine-grained token cannot reach another organisation's repositories)."
  exit 1
fi

# The description is the module's display name there; the maintainer role may not be allowed it.
gh api -X PATCH "repos/$repo" -f description="Re: TeleVIP" -f homepage="https://github.com/$GITHUB_REPOSITORY" >/dev/null \
  || echo "::warning::Could not set the description of $repo; set it to \"Re: TeleVIP\" by hand."

mkdir -p build/modules-repo
cp .github/modules-repo/SUMMARY build/modules-repo/SUMMARY
REPO="$GITHUB_REPOSITORY" BRANCH="${GITHUB_REF_NAME:-main}" python3 - <<'PY'
import os, re
repo, branch = os.environ["REPO"], os.environ["BRANCH"]
def absolute(url, image):
    if re.match(r"^(https?:|mailto:|#|data:)", url): return url
    if url.startswith("../../"): return f"https://github.com/{repo}/" + url[6:]
    if image: return f"https://raw.githubusercontent.com/{repo}/{branch}/{url}"
    return f"https://github.com/{repo}/blob/{branch}/{url}"
text = open("README.md", encoding="utf-8").read()
text = re.sub(r'(<img\b[^>]*\bsrc=")([^"]+)"', lambda m: m.group(1) + absolute(m.group(2), True) + '"', text)
text = re.sub(r'(\bhref=")([^"]+)"', lambda m: m.group(1) + absolute(m.group(2), False) + '"', text)
text = re.sub(r'(!\[[^\]]*\]\()([^)\s]+)\)', lambda m: m.group(1) + absolute(m.group(2), True) + ")", text)
# Images are absolute by now, so whatever ](...) is left is a link - badges included: [![..](..)](link)
text = re.sub(r'(\]\()([^)\s]+)\)', lambda m: m.group(1) + absolute(m.group(2), False) + ")", text)
open("build/modules-repo/README.md", "w", encoding="utf-8").write(text)
PY

for f in SUMMARY README.md; do
  local_sha=$(git hash-object "build/modules-repo/$f")
  remote_sha=$(gh api "repos/$repo/contents/$f" --jq .sha 2>/dev/null || true)
  [ "$local_sha" = "$remote_sha" ] && continue
  args=(-X PUT "repos/$repo/contents/$f" -f message="Update $f for $name"
        -f content="$(base64 -w0 "build/modules-repo/$f")")
  [ -n "$remote_sha" ] && args+=(-f sha="$remote_sha")
  gh api "${args[@]}" >/dev/null
done

if gh release view "$tag" --repo "$repo" >/dev/null 2>&1; then
  gh release upload "$tag" "$apk" --repo "$repo" --clobber
  gh release edit "$tag" --repo "$repo" --title "$name" --notes-file notes.md
else
  gh release create "$tag" "$apk" --repo "$repo" --title "$name" --notes-file notes.md --latest
fi
echo "Published $tag to https://github.com/$repo"
