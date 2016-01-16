#!/bin/sh

# if this chart is called X.chart.sh, then all functions and global variables
# must start with X_

# _update_every is a special variable - it holds the number of seconds
# between the calls of the _update() function
example_update_every=

# _check is called once, to find out if this chart should be enabled or not
example_check() {
	# this should return:
	#  - 0 to enable the chart
	#  - 1 to disable the chart

	return 0
}

# _create is called once, to create the charts
example_create() {
	# create the chart with 3 dimensions
	cat <<EOF
CHART example.random '' "Random Numbers Stacked Chart" "% of random numbers" random random stacked 5000 $example_update_every
DIMENSION random1 '' percentage-of-absolute-row 1 1
DIMENSION random2 '' percentage-of-absolute-row 1 1
DIMENSION random3 '' percentage-of-absolute-row 1 1
CHART example.random2 '' "A random number" "random number" random random area 5001 $example_update_every
DIMENSION random '' absolute 1 1
EOF

	return 0
}

# _update is called continiously, to collect the values
example_last=0
example_count=0
example_update() {
	local value1 value2 value3 value4 mode

	# the first argument to this function is the microseconds since last update
	# pass this parameter to the BEGIN statement (see bellow).

	# do all the work to collect / calculate the values
	# for each dimension
	# remember: KEEP IT SIMPLE AND SHORT

	value1=$RANDOM
	value2=$RANDOM
	value3=$RANDOM
	value4=$[8192 + (RANDOM * 16383 / 32767) ]

	if [ $example_count -gt 0 ]
		then
		example_count=$[example_count - 1]

		[ $example_last -gt 16383 ] && value4=$[example_last + (RANDOM * ( (32767 - example_last) / 2) / 32767)]
		[ $example_last -le 16383 ] && value4=$[example_last - (RANDOM * (example_last / 2) / 32767)]
	else
		example_count=$[1 + (RANDOM * 5 / 32767) ]

		[ $example_last -gt 16383 -a $value4 -gt 16383 ] && value4=$[value4 - 16383]
		[ $example_last -le 16383 -a $value4 -lt 16383 ] && value4=$[value4 + 16383]
	fi
	example_last=$value4

	# write the result of the work.
	cat <<VALUESEOF
BEGIN example.random $1
SET random1 = $value1
SET random2 = $value2
SET random3 = $value3
END
BEGIN example.random2 $1
SET random = $value4
END
VALUESEOF
	# echo >&2 "example_count = $example_count value = $value4"

	return 0
}
