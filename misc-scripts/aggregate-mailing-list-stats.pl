#! /usr/bin/perl

# To operate over a set of *.members and *.traffic files
# Ask #debian-lists (formorer) for them if update is needed
# They're the source used for generating https://lists.debian.org/stats/

foreach (<*.members>) {
    next if /REMOVED/;
    next if /DISABLED/;
    next if /private/;
    next unless /^debian/;
    s/.members$//;
    push @lists, $_
}

# @lists = "debian-devel";

foreach $list (@lists) {
    next unless -e "$list.traffic";

    open M, "$list.members";
    while ($l = <M>) {
	chomp $l;

	($a, $b) = split (/\s+/, $l);
	$members{$list}{$a} = $b;
    }

    open T, "$list.traffic";
    while ($l = <T>) {
	chomp $l;

	($a, $b) = split (/\s+/, $l);
	$traffic{$list}{$a} = $b;
    }

    ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime(time);

    $fmt = "%04d%02d";
    $year += 1900;
    if ($mon == 0) { # January
	$firstmonth = sprintf($fmt, $year -1, 1);
	$lastmonth = sprintf($fmt, $year -1, 12);
    } else {
	$firstmonth = sprintf($fmt, $year -1, $mon +1);
	$lastmonth = sprintf($fmt, $year, $mon);
    }

    foreach $month ($firstmonth..$lastmonth) {
	next unless $month =~ /0[1-9]$/ or $month =~ /1[012]$/ ;
	$total_members += $members{$list}{$month};
	$total_traffic_in += $traffic{$list}{$month};
	$total_traffic_out += $traffic{$list}{$month} * $members{$list}{$month};
    }
}

print "lists: ".($#lists+1)."\n";
# print "total members: $total_members\n";
$avg_members = $total_members / 12;
printf "average members: %.1f\n", $avg_members;

print "\n";

printf "total traffic_in: %d\n", $total_traffic_in;
$avg_traffic_in_monthly = $total_traffic_in / 12;
printf "average traffic_in_monthly: %.1f\n", $avg_traffic_in_monthly;
$avg_traffic_in_daily = $total_traffic_in / 365;
printf "average traffic_in_daily: %.1f\n", $avg_traffic_in_daily;

print "\n";

printf "total traffic_out: %d\n", $total_traffic_out;
$avg_traffic_out_monthly = $total_traffic_out / 12;
printf "average traffic_out_monthly: %.1f\n", $avg_traffic_out_monthly;
$avg_traffic_out_daily = $total_traffic_out / 365;
printf "average traffic_out_daily: %.1f\n", $avg_traffic_out_daily;

