#!/bin/bash

#############################################################
# @author: Brice Pajot (bpajot@bluexml.com)
# @date: 2014-11-21
# @version: 1.0
# 
#############################################################

CURRENT_DIR=$(dirname $(readlink -f $0))

UNZIP=${UNZIP:-unzip}

OUTPUT=

WORKING_DIR= # default to /tmp
JOB_DIR=
DEBUG=0
STDOUT=/dev/stdout
KEEP_JOB_DIR=0
DISCARD_MODULE=0

function usage() {
	echo "Deploy an amp module to a target directory."
	echo " Usage: $(basename $0) [options] amp-file target"
	echo " where options:"
	echo " --help, -h                : display this help"
	echo "  -workdir WORKDIR         : directory where intermediate computation material will be stored" 
	echo "  --output OUTPUT-FILE, -o : define the output-file name and path"
	echo "  -d, --debug              : activate debug mode"
	echo "  -kjd, --keep-job-dir     : keep the job-directory after processing"
	echo "  -dm, --discard-module    : discard the module feature"
}

while [ "${2+defined}" ]; do

	case "$1" in
	
	--help|-h)
		usage
		exit 0
	;;
	
	--workdir|-wd)
		shift
		WORKING_DIR=$1
		if [ ! -d ${WORKING_DIR} ]; then
			echo "The working directory ${WORKING_DIR} is not a valid (existing) directory"
			exit
		fi
	;;
	
	--output|-o)
		shift
		OUTPUT=$1
	;;
	
	--debug|-d)
		DEBUG=1
	;;
	
	--keep-job-dir|-kjd)
		KEEP_JOB_DIR=1
	;;
	
	--discard-module|-dm)
		DISCARD_MODULE=1
	;;
	
	*)
		DOCUMENT=$1
		TARGET_DIR=$2
	;;
	
	esac	
	
	shift
	
done

#############################################################
# UTILITY FUNCTIONS
# 
#############################################################

function printc() {
	
	output=$(eval 'echo "$1"')
	echo -ne "${output}" >> ${2:-${STDOUT}}
	
}

function log() {

	if [ ${DEBUG:-0} -lt 1 ]; then return; fi
	printc "$*"
	
}

function exit_on_failure() {
	
	status=$?
	if [ ${status} != 0 ]; then
		printc "Failure of operation: $1\n" /dev/stderr
		exit ${2:--1}
	fi
	
}

function exit_with_failure() {
	
	printc "${1:-"Failure"}\n" /dev/stderr
	exit ${2:--1}
	
}

function check_executable() {
	
	if [ ! $(which "$1" -s) ]; then
		return 1
	fi

}

function cleanup() {
	
	if [ 1 -eq ${KEEP_JOB_DIR} ]; then return; fi
	
	log "Cleaning-up job-directory ${JOB_DIR}\n"
	rm -rf ${JOB_DIR}
	
}

#############################################################
# MAIN
#############################################################

# The default AMP => WAR file mappings
function generate_default_file_mapping() {

local output=${1:-"file-mapping.properties"}

cat << EOF > ${output}
/config=/WEB-INF/classes
/lib=/WEB-INF/lib
/licenses=/WEB-INF/licenses
/web/jsp=/jsp
/web/css=/css
/web/images=/images
/web/scripts=/scripts
/web/php=/php
EOF

}

function complete_file_mapping() {

	local include_default=$(cat file-mapping.properties | grep 'include.default' | cut -f 2 -d '=')
	test "${include_default}" == "false" && return;

	generate_default_file_mapping "file-mapping-default.properties"
	cat file-mapping-default.properties | while read mapping; do
		local source="$(echo ${mapping} | cut -f 1 -d '=')"
		test ! "$(grep "${source}=" file-mapping.properties)" && echo "${mapping}" >> file-mapping.properties
	done

}

function actual_target() {
	local input=$1
	local mapped=0
	cat file-mapping.properties | while read line; do
		local source="$(echo ${line} | cut -f 1 -d '=')"
		if [ "$(echo "${input}" | grep "${source}")" ]; then
			local target="$(echo ${line} | cut -f 2 -d '=')"
			local escaped_source="$(echo ${source} | sed 's/\//\\\//g')"
			local escaped_target="$(echo ${target} | sed 's/\//\\\//g')"
			eval echo "$(echo ${input} | sed s/${escaped_source}/${escaped_target}/g)"
			exit
		fi
	done
}

function sync_source_to_target() {
	local source=$1
	local target=$2
	mkdir -p ${target}
	rsync -avzPcC ${source}/* ${target}/
}

# Put the module.properties besides the module-context.xml if it exists,
# else create a module-directory based on the module-id
function copy_module_properties() {
	local module_context=$(find . | grep module-context.xml)
	local target=""
	if [ "${module_context}" ]; then
		target=$(dirname "${module_context}")
	else
		local module_id=$(cat module.properties | grep "module.id" | cut -f 2 -d '=' | tr -d -c [A-Za-z0-9_\\-])
		target="config/alfresco/module/${module_id}"
	fi
	
	mkdir -p ${target}
	mv module.properties ${target}/
}

function main() {
	
	local input_file="$(readlink -f "$1")"
	local output_file="${2:-${input_file%.*}.jar}"
	
	pushd ${JOB_DIR} > /dev/null
	
	unzip ${input_file} > /dev/null

	if [ ! -e file-mapping.properties ]; then
		generate_default_file_mapping
	else
		complete_file_mapping
	fi

	test ${DISCARD_MODULE} -eq 0 && test -e module.properties && copy_module_properties

	cat file-mapping.properties | while read mapping; do
		local source="$(echo ${mapping} | cut -f 1 -d '=')"
		local target="$(echo ${mapping} | cut -f 2 -d '=')"
		echo "Processing source ${source}"
		test -d ".${source}" && sync_source_to_target ".${source}" "${TARGET_DIR}${target}"
	done

	popd > /dev/null
	
}

if [ "x${DOCUMENT}" == "x" -o "x${TARGET_DIR}" == "x" ]; then
	usage
	exit -1
fi

if [ ! -d "${TARGET_DIR}" ]; then
	echo "The target directory ${TARGET_DIR} has to be a valid existing directory!" > /dev/stderr
	exit 2
fi

DOCUMENT=${DOCUMENT%.*}.amp

trap 'cleanup; exit' SIGHUP SIGINT SIGQUIT SIGTERM

JOB_DIR=$(mktemp -d -t amp2jarXXXXXXX.job -p ${WORKING_DIR:-/tmp})
test -x ${JOB_DIR} || exit_with_failure "Cannot create the job directory"
log "Created job-directory ${JOB_DIR}\n"

main ${DOCUMENT} ${OUTPUT}

cleanup

#############################################################
# END
#############################################################
