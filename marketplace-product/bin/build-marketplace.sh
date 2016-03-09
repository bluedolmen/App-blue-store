#!/bin/bash

CURRENT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source "${CURRENT_DIR}/common.sh.lib"

PRODUCT_NAME=marketplace
BRANCH=5.0

APPSTORE_CORE_LOCATION=${APPSTORE_CORE_LOCATION:-${WORKSPACE_DIR}/org.bluedolmen.appstore-core}
ALFRESCO_EXTENSIONS_LOCATION=${ALFRESCO_EXTENSIONS_LOCATION:-${WORKSPACE_DIR}/alfresco-extensions}

DEPENDENCY_PROJECTS=("${APPSTORE_CORE_LOCATION}")
REPO_PROJECTS=("${ALFRESCO_EXTENSIONS_LOCATION}" "${ROOT_DIR}/marketplace-commons" "${ROOT_DIR}/marketplace-repo")
SHARE_PROJECTS=("${ROOT_DIR}/marketplace-share")

source "${CURRENT_DIR}/build.sh"

