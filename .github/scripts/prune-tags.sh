#!/usr/bin/env bash
# Deletes every tag of a repository except <keep>.
#   prune-tags.sh <owner/repo> <keep>
set -euo pipefail

repo=$1 keep=$2
tags=$(gh api --paginate "repos/$repo/git/matching-refs/tags" --jq '.[].ref | sub("^refs/tags/"; "")')
if ! grep -qxF "$keep" <<< "$tags"; then
  echo "::error::$repo has no tag $keep; nothing deleted."
  exit 1
fi
while read -r tag; do
  [ "$tag" = "$keep" ] && continue
  gh api -X DELETE "repos/$repo/git/refs/tags/$tag"
  echo "Deleted $repo tag $tag"
done <<< "$tags"
echo "Kept $repo tag $keep"
