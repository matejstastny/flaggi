#!/usr/bin/env bash
set -euo pipefail

AUTHOR='Matej Stastny'
LICENSE='MIT'
LINK='https://github.com/matejstastny/flaggi'

log() {
    local level="$1"
    shift
    printf "[%s] %s\n" "$level" "$*"
}

export AUTHOR LICENSE LINK

log INFO "Scanning for .java files…"

find . -type f -name '*.java' -print0 |
    while IFS= read -r -d '' file; do
        perl -0777 -e '
    my $file = shift;
    local $/ = undef;
    open my $fh, "<", $file or die "open $file: $!";
    my $text = <$fh>;
    close $fh;

    if ($text =~ m{/\*(?:.*?)\*/}s) {
      my $block = $&;
      if ($block =~ /Author:|Date|GitHub/i) {
        my ($bm,$bd,$by) = $block =~ /([0-1]?\d)\/([0-3]?\d)\/([0-9]{4})/;
        my $base = defined $bm ? sprintf("%04d-%02d-%02d",$by,$bm,$bd) : "";

        my ($vn,$vm,$vd,$vy) = $block =~ /v(\d+)\s*[-:]\s*([0-1]?\d)\/([0-3]?\d)\/([0-9]{4})/i;
        my $vpart = "";
        if (defined $vn && $vn ne "") {
          $vpart = " ($vn.0: " . sprintf("%02d-%02d-%04d",$vm,$vd,$vy) . ")";
        }

        my $fname = $file; $fname =~ s{.*/}{};
        my $hdr = join("\n",
          "// ------------------------------------------------------------------------------",
          "// $fname - description TODO",
          "// ------------------------------------------------------------------------------",
          "// Author: " . ($ENV{AUTHOR} || "AUTHOR"),
          "// Date: " . ($base || "(unknown)") . $vpart,
          "// License: " . ($ENV{LICENSE} || "LICENSE"),
          "// Link: " . ($ENV{LINK} || "LINK"),
          "// ------------------------------------------------------------------------------",
          "",
          ""
        );

        $text =~ s/\Q$block\E//;
        $text =~ s/\A(?:\s*\n)+//s;
        $text = $hdr . $text;

        open my $out, ">", $file or die "open out $file: $!";
        print $out $text;
        close $out;
        print "[OK] Updated: $file\n";
      } else {
        print "[SKIP] No matching header: $file\n";
      }
    } else {
      print "[SKIP] No block comment header: $file\n";
    }
  ' "$file"
    done

log INFO "Done."
