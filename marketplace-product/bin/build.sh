#!/bin/bash

CURRENT_DIR=${CURRENT_DIR:-"$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"}
SVNREV="$(svn_rev "${WORKSPACE_DIR}")"
MVN_OPTS="${MVN_OPTS} -Dbuild.svnrev=${SVNREV}"
MVN_OPTS_="${MVN_OPTS}"
TAR="tar"
TAR_ARCHIVE_OPTS="cfz"

WEBAPPS_PACKAGING=1

#########################################################
# PROJECT BUILDS
#########################################################

# The root-dir contains the parent-pom which needs to be installed 
# -N means non-recursive
if [ -e "${ROOT_DIR}/pom.xml" ]; then
	cd ${ROOT_DIR}
	${MAVEN} -N clean install
fi

# Dependency project are installed without recursion too
for project in "${DEPENDENCY_PROJECTS[@]}"; do

	test ! -d ${project} && continue
	cd "${project}"
	${MAVEN} -N ${MVN_OPTS} clean install || exit_with_failure "Error while building dependency-project $(basename "${project}")"

done

PROJECTS=("${REPO_PROJECTS[@]}" "${SHARE_PROJECTS[@]}" "${EXTRA_WEBAPPS_PROJECTS[@]}")

for project in "${REPO_PROJECTS[@]}"; do

	test ! -d ${project} && continue
	cd "${project}"
	${MAVEN} ${MVN_OPTS} -Prepo-extension,dependency-jar clean install || exit_with_failure "Error while building $(basename "${project}")"
#	${MAVEN} ${MVN_OPTS}  install || exit_with_failure "Error while installing $(basename "${project}")"

done

for project in "${SHARE_PROJECTS[@]}"; do

	test ! -d ${project} && continue
	cd "${project}"
	${MAVEN} ${MVN_OPTS} -Pshare-extension clean package || exit_with_failure "Error while building $(basename "${project}")"

done

for project in "${EXTRA_WEBAPPS_PROJECTS[@]}"; do

	test ! -d ${project} && continue
	cd "${project}"
	${MAVEN} ${MVN_OPTS} clean package || exit_with_failure "Error while building $(basename "${project}")"

done

WEBAPPS_DIRNAME=webapps
TARGET=${ROOT_DIR}/target/${WEBAPPS_DIRNAME}

#########################################################
# AMPS PROCESSING
# + ALFRESCO WEBAPP BUILD
#########################################################

AMPS_DIRNAME="amps"
AMPS_DIR="${ROOT_DIR}/target/${AMPS_DIRNAME}"
mkdir -p "${AMPS_DIR}"

for project in "${REPO_PROJECTS[@]}"; do
	mv "${project}"/target/*.amp "${AMPS_DIR}" || exit_with_failure "Cannot find amp files in $(basename "${project}")"
done

if [ ${WEBAPPS_PACKAGING} -eq 1 ]; then
	TARGET_WEBAPP="${TARGET}/alfresco"
	mkdir -p "${TARGET_WEBAPP}"
	for amp_file in "${AMPS_DIR}"/*.amp; do
		${APPLY_AMP} "${amp_file}" "${TARGET_WEBAPP}"
	done
fi

#########################################################
# SHARE WEBAPP BUILD
#########################################################

AMPS_SHARE_DIRNAME="amps_share"
AMPS_DIR="${ROOT_DIR}/target/${AMPS_SHARE_DIRNAME}"
mkdir -p "${AMPS_DIR}"

for project in "${SHARE_PROJECTS[@]}"; do
	mv "${project}"/target/*.amp "${AMPS_DIR}" || exit_with_failure "Cannot find amp files in $(basename "${project}")"
done

if [ ${WEBAPPS_PACKAGING} -eq 1 ]; then
	TARGET_WEBAPP="${TARGET}/share"
	mkdir -p "${TARGET_WEBAPP}"
	for amp_file in "${AMPS_DIR}"/*.amp; do
		${APPLY_AMP} "${amp_file}" "${TARGET_WEBAPP}"
	done
fi

#########################################################
# EXTRA WEBAPPS PROJECTS
#########################################################

for project in "${EXTRA_WEBAPPS_PROJECTS[@]}"; do
	mv "${project}"/target/*.war "${TARGET}" || exit_with_failure "Cannot find war files in $(basename "${project}")"
done

#########################################################
# PACKAGING
#########################################################

cd ${TARGET}/.. # parent of webapps
# WARN! It cannot work if the version is inherited, we should use something like:
# mvn -q -Doutput=/dev/stdout help:effective-pom | xpath -q -e '/project/version/text()'
VERSION="$(${XPATH} -q -e '/project/version/text()' ${ROOT_DIR}/pom.xml)"
webapps_archive_file="webapps-${PRODUCT_NAME}-${VERSION:-${TODAY}}${SVNREV:+-}${SVNREV}.tar.gz"
amps_archive_file="amps-${PRODUCT_NAME}-${VERSION:-${TODAY}}${SVNREV:+-}${SVNREV}.tar.gz"

# Write build-properties into the provided file
if [ -f "${BUILD_PROPERTIES}" ]; then
	cat >> "${BUILD_PROPERTIES}" << EOF
application.revision=${SVNREV}
application.build-date=${TODAY}
EOF
fi

# Build the archive
test ${WEBAPPS_PACKAGING} -eq 1 && ${TAR} ${TAR_ARCHIVE_OPTS} "${webapps_archive_file}" "${WEBAPPS_DIRNAME}"
${TAR} ${TAR_ARCHIVE_OPTS} "${amps_archive_file}" "${AMPS_DIRNAME}" "${AMPS_SHARE_DIRNAME}" $(ls "${WEBAPPS_DIRNAME}/*.war" 2>/dev/null)

#########################################################
# PUBLISHING
#########################################################

if [ -d ${DELIVERY_DIR} ]; then

	if [ ${WEBAPPS_PACKAGING} -eq 1 ]; then
		cp ${webapps_archive_file} ${DELIVERY_DIR}
		keep_last_n_files "${DELIVERY_DIR}" 5 "webapps-${PRODUCT_NAME}" # Only keep 5 versions
	fi

	cp ${amps_archive_file} ${DELIVERY_DIR}
	keep_last_n_files "${DELIVERY_DIR}" 5 "amps-${PRODUCT_NAME}" # Only keep 5 versions

fi

