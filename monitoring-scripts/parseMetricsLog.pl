#!/usr/bin/perl
use strict;

# metrics group to tag keys mapping
my %metricsGroupTagKeys;
# metrics group to metric names mapping
my %metricsGroupMetricNames;
while(<>){
  # skip timestamp and get metrics prefix
  my $metricGroup;
  my $metrics;
  if(/[0-9]+\s{1}([^:]+):(.*)/){
    $metricGroup = $1;
    $metrics = $2;
  }

  my %keyvalues;
  $_=$metrics;
  while(/\s{1}([^=]+)=([^,]+)/g){
    $keyvalues{$1}=$2;
  }

  #### ensure we have tag keys and metric names as many as possible
  my %tagKeys;
  my %metricNames;

#  print "metricGroup: " . $metricGroup ."\n";

  foreach my $key (keys %keyvalues){
    ### numeric value comes from metrics instead of tag
    if($key != "port" || $keyvalues{$key} =~ /^[0-9]+[\.]*[0-9]*/){
      $metricNames{$key}="1";
    }else{
        $tagKeys{$key}="1";
    }
 #   print "key=" . $key . ", value=" . $keyvalues{$key} . "\n";
  }
  $metricsGroupMetricNames{$metricGroup}=\%metricNames;
  $metricsGroupTagKeys{$metricGroup}=\%tagKeys;
}

# output with a predefined format
foreach my $key (keys %metricsGroupTagKeys){
  print "MetricGroup: " . $key . "\n";

  my %tagKeys = %{$metricsGroupTagKeys{$key}};
  print "\tTags:";
  foreach my $tagKey (sort keys %tagKeys){
    print $tagKey . ",";
  }
  print "\n";

  my %metricNames = %{$metricsGroupMetricNames{$key}};
  print "\tMetrics:";
  foreach my $metricName (sort keys %metricNames){
    print $metricName . ",";
  }
  print "\n";
}
