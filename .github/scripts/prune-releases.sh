#!/usr/bin/env bash
# Deletes every release of a repository except the one tagged <keep>; tags are left alone.
#   prune-releases.sh <owner/repo> <keep>
set -euo pipefail

repo=$1 keep=$2
releases=$(gh api --paginate "repos/$repo/releases?per_page=100" --jq '.[] | [.id, .tag_name] | @tsv')
if ! cut -f2 <<< "$releases" | grep -qxF "$keep"; then
  echo "::error::$repo has no release tagged $keep; nothing deleted."
  exit 1
fi
while IFS=$'\t' read -r id tag; do
  [ "$tag" = "$keep" ] && continue
  gh api -X DELETE "repos/$repo/releases/$id"
  echo "Deleted $repo release $tag"
done <<< "$releases"
echo "Kept $repo release $keep"
