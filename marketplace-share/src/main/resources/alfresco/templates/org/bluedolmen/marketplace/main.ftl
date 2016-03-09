<#include "/org/alfresco/include/alfresco-template.ftl" />

<@templateHeader >
	<link rel="stylesheet" type="text/css" href="/extjs/resources/css/ext-all-neptune.css" />
	<@link rel="stylesheet" type="text/css" href="${url.context}/res/marketplace/resources/css/ext-custom.css" />
	<@link rel="stylesheet" type="text/css" href="${url.context}/res/marketplace/resources/css/lightbox.css" />
	<@link rel="stylesheet" type="text/css" href="${url.context}/res/marketplace/resources/jquery.bxslider.css" />

	<script type="text/javascript" src="//code.jquery.com/jquery-1.11.1.min.js"></script>
	<script src="${url.context}/res/marketplace/resources/jquery.bxslider.min.js"></script>
	<script src="${url.context}/res/marketplace/resources/lightbox.min.js"></script>
	
	<script type="text/javascript" src="${url.context}/res/marketplace/resources/markdown.min.js"></script>
	
	<!-- TODO: This library should be shared between all the client applications -->
	<script type="text/javascript" src="${url.context}/res/marketplace/resources/i18next-1.7.4.min.js"></script>
	<script type="text/javascript">
	
		var options = {
			resGetPath : '${url.context}/res/marketplace/locales/__lng__/__ns__.json',
			lng : 'en-US',
			ns: { 
				namespaces: ['mp'], 
				defaultNs: 'mp'
			}
		};
		i18n.init(options);
		
	</script>
	
	<script type="text/javascript" src="/extjs/ext-dev.js"></script>
	<script type="text/javascript" src="/extjs/locale/ext-lang-${lang!"fr"}.js"></script>	
	<script type="text/javascript" src="${url.context}/res/marketplace/app.js"></script>
	
</@>


<body>

	<@region id="extensions" scope="page"/>
	<@region id="appstore" scope="template"/>

	<div id="loading-mask"></div>
	<div id="loading"></div>

</body>
