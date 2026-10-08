#!/usr/bin/env bash
# Prints a client's newest builds, newest first, one "<label><TAB><APK URL>" per line.
#
#   client-builds.sh github <owner/repo> <asset pattern> [count] [only]
#       the latest releases that carry an APK; the asset matching the pattern (case-insensitive
#       regex, e.g. arm64) is preferred, else the release's first APK. With "only", releases
#       without a matching APK are skipped instead (for a repo that ships several flavours).
#       Needs gh and GH_TOKEN.
#   client-builds.sh fdroid <package> [count]
#       the latest versions on f-droid.org, its archive included, as arm64 or universal APKs.
#   client-builds.sh url <URL>
#       a single build at a fixed address (only ever the newest one, e.g. telegram.org).
set -euo pipefail

source=$1
case "$source" in
  github)
    repo=$2 asset=$3 count=${4:-5} only=${5:-}
    gh api "repos/$repo/releases?per_page=50" | jq -r --arg asset "$asset" --argjson n "$count" \
        --argjson only "$([ "$only" = only ] && echo true || echo false)" '
      [ .[] | select(.draft | not)
        | {tag: .tag_name, apks: [.assets[] | select(.name | test("\\.apk$"; "i"))]}
        | .matching = (.apks | map(select(.name | test($asset; "i"))))
        | select(if $only then .matching | length > 0 else .apks | length > 0 end)
        | .tag + "\t" + ((.matching + .apks) | .[0].browser_download_url) ]
      | .[:$n][]'
    ;;
  fdroid)
    package=$2 count=${3:-5}
    work=$(mktemp -d)
    for repo in repo archive; do
      curl -fsSL --retry 3 --retry-all-errors -o "$work/$repo.jar" "https://f-droid.org/$repo/index-v1.jar"
      unzip -p "$work/$repo.jar" index-v1.json | jq -r --arg p "$package" --arg base "https://f-droid.org/$repo/" '
        .packages[$p] // [] | .[]
        | select((.nativecode // ["arm64-v8a"]) | index("arm64-v8a"))
        | [(.versionCode | tostring), .versionName, $base + .apkName] | @tsv'
    done | sort -t $'\t' -k1,1rn | awk -F '\t' -v n="$count" '!seen[$2]++ && shown++ < n { print $2 "\t" $3 }'
    # (awk counts rather than head: head would close the pipe early, which pipefail reports.)
    rm -rf "$work"
    ;;
  url)
    printf 'latest\t%s\n' "$2"
    ;;
  *)
    echo "unknown source: $source" >&2
    exit 2
    ;;
esac
