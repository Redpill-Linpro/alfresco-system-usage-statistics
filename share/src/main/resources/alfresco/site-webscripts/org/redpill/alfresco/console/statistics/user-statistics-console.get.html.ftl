<@markup id="css" >
  <#-- CSS Dependencies -->
  <@link rel="stylesheet" type="text/css" href="${url.context}/res/rl/components/console/user-statistics-console.css"  group="console" />
  <@link href="${url.context}/res/modules/documentlibrary/global-folder.css" group="console"/>
</@>

<@markup id="js">
	<@script type="text/javascript" src="${url.context}/res/yui/datasource/datasource.js" group="console" />
	<@script type="text/javascript" src="${url.context}/res/components/console/consoletool.js" group="console" />
	<@script type="text/javascript" src="${url.context}/res/rl/components/console/user-statistics-console.js"  group="console" />
	<@script type="text/javascript" src="${url.context}/res/modules/documentlibrary/global-folder.js" group="console" />
	<@script type="text/javascript" src="${url.context}/res/yui/resize/resize.js" group="console" />
</@>

<@markup id="widgets">
  <@createWidgets group="console"/>
</@>

<@markup id="html">
  
  <#assign el=args.htmlid?html>
  
  <div id="${el}-body" class="statistics-console">
      <div id="${el}-main" class="hidden">
          <div>
              <div class="header-bar">${msg("statistics-users.label")}</div>
              <div>
                <div id="${el}-statistics-control" class="-statistics-control">
                  <label for="year">${msg("label.year")}</label>
                  <select name="year" id="${el}-statistics-control-year">
                    <option></option>
                  </select>
                  <div id="${el}-statistics-select-list" class="-statistics-select-list">
                  </div>
                </div>
              </div>
              <div>
              	<div class="header-bar">${msg("statistics-users-internal.label")}</div>
                  <div id="${el}-statistics-internal-users-list" class="-statistics-internal-users-list"></div>
                  <div class="header-bar">${msg("statistics-users-external.label")}</div>
                  <div id="${el}-statistics-external-users-list" class="-statistics-external-users-list"></div>
              </div>
          </div>
      </div>
  </div>
</@>