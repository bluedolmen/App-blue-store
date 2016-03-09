<#include "/org/alfresco/include/alfresco-template.ftl" />

<@templateHeader >
	<link rel="stylesheet" type="text/css" href="/extjs/resources/css/ext-all-neptune.css" />
	<@link rel="stylesheet" type="text/css" href="${url.context}/res/module-builder/resources/css/ext-custom.css" />
	<@link rel="stylesheet" type="text/css" href="${url.context}/res/marketplace/resources/css/ext-custom.css" />

	<!-- TODO: This library should be shared between all the client applications -->
	<script type="text/javascript" src="${url.context}/res/module-builder/resources/i18next-1.7.4.min.js"></script>
	<script type="text/javascript">
/*	
		var originalFetchOne = i18n.sync._fetchOne;
		i18n.sync._fetchOne = function (lng, ns, options, done) {
		    // Could enhance this to deal with multi-level namespaces like ns1:ns2:key etc
		    if (ns == 'mp') {
		        options.resGetPath = '${url.context}/res/marketplace/locales/__lng__/__ns__.json';
		    }
		    return originalFetchOne(lng, ns, options, done);
		}
		
		var options = {
			resGetPath : '${url.context}/res/module-builder/locales/__lng__/__ns__.json',
			lng : 'en-US',
			ns: { 
				namespaces: ['mb','mp'], 
				defaultNs: 'mb'
			}			
		};
		i18n.init(options);
*/		
	</script>
	
	<script type="text/javascript" src="/extjs/ext-dev.js"></script>
	<script type="text/javascript" src="/extjs/locale/ext-lang-${lang!"fr"}.js"></script>
	<script type="text/javascript" src="${url.context}/res/module-builder/app.js"></script>
	
</@>


<body>

	<@region id="extensions" scope="page"/>
	<@region id="appstore" scope="page"/>

	<div id="loading-mask"></div>
	<div id="loading"></div>

</body>
