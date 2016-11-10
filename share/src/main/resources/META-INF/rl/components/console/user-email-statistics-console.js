/**
 * RL root namespace.
 * 
 * @namespace RL
 */
// Ensure RL root object exists
if (typeof RL == "undefined" || !RL) {
  var RL = {};
}
/**
 * Admin Console User Statistics Console
 * 
 * @namespace Alfresco
 * @class RL.SiteStatisticsConsole
 */
(function() {
  /**
   * YUI Library aliases
   */
  var Dom = YAHOO.util.Dom;
  var $html = Alfresco.util.encodeHTML;
  var Bubbling = YAHOO.Bubbling;
  /**
   * UserEmailStatisticsConsole constructor.
   * 
   * @param {String}
   *            htmlId The HTML id of the parent element
   * @return {RL.UserEmailStatisticsConsole} The new UserEmailStatisticsConsole instance
   * @constructor
   */
  RL.UserEmailStatisticsConsole = function(htmlId) {
    this.name = "RL.UserEmailStatisticsConsole";
    RL.UserEmailStatisticsConsole.superclass.constructor.call(this, htmlId);
    /* Register this component */
    Alfresco.util.ComponentManager.register(this);
    /* Load YUI Components */
    Alfresco.util.YUILoaderHelper.require(["button", "container", "datasource", "datatable", "paginator", "json", "history"], this.onComponentsLoaded, this);
    /* Define panel handlers */
    var parent = this;
    this.data = {
      "selectionData": new Object(),
      "siteData": new Object()
    };
    // loading message function
    this.loadingMessage = null;
    this.fnShowLoadingMessage = function() {
      parent.loadingMessage = Alfresco.util.PopupManager.displayMessage({
        displayTime: 0,
        text: '<span class="wait">' + $html(parent.msg("message.loading")) + '</span>',
        noEscape: true
      });
    };
    // slow data webscript message
    this.timerShowLoadingMessage = null;
    /* File List Panel Handler */
    ListPanelHandler = function ListPanelHandler_constructor() {
      ListPanelHandler.superclass.constructor.call(this, "main");
    };
    this.LoadAvailableYears = function() {
      var selectBox = Dom.get(this.id + "-statistics-control-year");
      selectBox.onchange = this.OnChangeYearFilter;
      this.timerShowLoadingMessage = YAHOO.lang.later(2000, this, this.fnShowLoadingMessage);
      Alfresco.util.Ajax.request({
        url: Alfresco.constants.PROXY_URI + "slingshot/doclib/treenode/node/alfresco/company/home/Data%20Dictionary/Redpill-Linpro/Statistics/UserEmailStatistics",
        responseContentType: Alfresco.util.Ajax.JSON,
        method: Alfresco.util.Ajax.GET,
        successCallback: {
          fn: this.LoadAvailableYears_success,
          scope: this
        },
        failureCallback: {
          fn: this.LoadAvailableYears_failure,
          scope: this
        }
      });
    };
    this.LoadAvailableYears_success = function(response) {
      if (this.timerShowLoadingMessage) {
        this.timerShowLoadingMessage.cancel();
      }
      if (this.loadingMessage) {
        try {
          this.loadingMessage.destroy();
        } catch (err) {

        }
      }
      if (response != null && response.json != null) {
        var items = response.json.items;
        var selectBox = Dom.get(this.id + "-statistics-control-year");
        for (var i = 0; i < items.length; i++) {
          var item = items[i];
          var option = document.createElement("option");
          option.value = item.name;
          option.innerHTML = item.name;
          selectBox.appendChild(option);
        }
      }
    };
    this.LoadAvailableYears_failure = function(response) {
      if (this.timerShowLoadingMessage) {
        this.timerShowLoadingMessage.cancel();
      }
      if (this.loadingMessage) {
        try {
          this.loadingMessage.destroy();
        } catch (err) {

        }
      }
    };
    this.OnChangeYearFilter = function(evt) {
      var option = evt.currentTarget.options[evt.currentTarget.selectedIndex];
      var target = option.value;
      parent.timerShowLoadingMessage = YAHOO.lang.later(2000, this, parent.fnShowLoadingMessage);
      Alfresco.util.Ajax.request({
        url: Alfresco.constants.PROXY_URI + "/slingshot/doclib2/doclist/all/node/alfresco/company/home/Data%20Dictionary/Redpill-Linpro/Statistics/UserEmailStatistics/" + target + "?sortField=cm%3acreated&sortAsc=false",
        responseContentType: Alfresco.util.Ajax.JSON,
        method: Alfresco.util.Ajax.GET,
        successCallback: {
          fn: parent.OnChangeYearFilter_success,
          scope: parent
        },
        failureCallback: {
          fn: parent.OnChangeYearFilter_failure,
          scope: parent
        }
      });
    };
    this.OnChangeYearFilter_success = function(response) {
      if (parent.timerShowLoadingMessage) {
        parent.timerShowLoadingMessage.cancel();
      }
      if (parent.loadingMessage) {
        try {
          this.loadingMessage.destroy();
        } catch (err) {

        }
      }
      if (response != null && response.json != null) {
        parent.data.selectionData = response.json;
        var successHandlerSelection = function(request, response, payload) {
          parent.widgets.dataTableSelection.onDataReturnInitializeTable.call(parent.widgets.dataTableSelection, request, response, payload);
        };
        var oCallbackSelection = {
          success: successHandlerSelection,
          failure: successHandlerSelection,
          scope: this.widgets.dataTableSelection,
          argument: this.widgets.dataTableSelection.getState()
        };
        this.widgets.dataSourceSelection.sendRequest("", oCallbackSelection);
      }
    };
    this.OnChangeYearFilter_failure = function(response) {
      if (parent.timerShowLoadingMessage) {
        parent.timerShowLoadingMessage.cancel();
      }
      if (parent.loadingMessage) {
        try {
          this.loadingMessage.destroy();
        } catch (err) {

        }
      }
    };
    this.SelectStatisticsRow = function(evt) {
      var nodeRef = this.getRecord(evt.target).getData().node.nodeRef;
      var nodeRefStr = nodeRef.replace("://", "/");
      parent.timerShowLoadingMessage = YAHOO.lang.later(2000, parent, parent.fnShowLoadingMessage);
      Alfresco.util.Ajax.request({
        url: Alfresco.constants.PROXY_URI + "api/node/" + nodeRefStr + "/content",
        responseContentType: Alfresco.util.Ajax.JSON,
        method: Alfresco.util.Ajax.GET,
        successCallback: {
          fn: parent.SelectStatisticsRow_success,
          scope: parent
        },
        failureCallback: {
          fn: parent.SelectStatisticsRow_failure,
          scope: parent
        }
      });
    };
    this.SelectStatisticsRow_success = function(response) {
      if (parent.timerShowLoadingMessage) {
        parent.timerShowLoadingMessage.cancel();
      }
      if (parent.loadingMessage) {
        try {
          this.loadingMessage.destroy();
        } catch (err) {

        }
      }
      if (response != null && response.json != null) {
        this.data.siteData.sites = response.json;
        this.data.siteData.startIndex = 0;
        this.data.siteData.totalRecords = response.json.length;
        var successHandlerInternal = function(request, response, payload) {
          parent.widgets.dataTableSite.onDataReturnInitializeTable.call(parent.widgets.dataTableSite, request, response, payload);
        };
        var oCallbackInternal = {
          success: successHandlerInternal,
          failure: successHandlerInternal,
          scope: this.widgets.dataTableSite,
          argument: this.widgets.dataTableSite.getState()
        };
        this.widgets.dataSourceSite.sendRequest("", oCallbackInternal);
      }
    };
    this.SelectStatisticsRow_failure = function(response) {
      if (parent.timerShowLoadingMessage) {
        parent.timerShowLoadingMessage.cancel();
      }
      if (parent.loadingMessage) {
        try {
          this.loadingMessage.destroy();
        } catch (err) {

        }
      }
    };
    YAHOO.extend(ListPanelHandler, Alfresco.ConsolePanelHandler, {
      /**
       * Called by YAHOO.lang.JSON.stringify(the
       * ConsolePanelHandler when this panel shall be
       * loaded
       * 
       * @method onLoad
       */
      onLoad: function onLoad() {
        // DataTable and DataSource setup
        parent.widgets.dataSourceSelection = new YAHOO.util.FunctionDataSource(function() {
          return YAHOO.lang.JSON.stringify(parent.data.selectionData);
        }, {
          "responseType": YAHOO.util.FunctionDataSource.TYPE_JSON,
          responseSchema: {
            resultsList: "items",
            metaFields: {
              recordOffset: "startIndex",
              totalRecords: "totalRecords"
            }
          }
        });
        // DataTable and DataSource setup
        parent.widgets.dataSourceSite = new YAHOO.util.FunctionDataSource(function() {
          return YAHOO.lang.JSON.stringify(parent.data.siteData);
        }, {
          "responseType": YAHOO.util.FunctionDataSource.TYPE_JSON,
          responseSchema: {
            resultsList: "sites",
            metaFields: {
              recordOffset: "startIndex",
              totalRecords: "totalRecords"
            }
          }
        });
        // Setup the main datatable
        this._setupDataSelectionTable();
        this._setupDataStatsTables();
        parent.LoadAvailableYears();
        //parent.LoadUserActivity();
      },
      _setupDataSelectionTable: function() {
        var renderCellDate = function(cell, record, column, data) {
          cell.innerHTML = $html(record.getData().node.properties["cm:created"].value);
        };
        var columnDefinitions = [{
          key: "node.properties.cm:created",
          label: parent._msg("select-statistics.label"),
          sortable: false,
          formatter: renderCellDate
        }];
        parent.widgets.dataTableSelection = new YAHOO.widget.DataTable(parent.id + "-statistics-select-list", columnDefinitions, parent.widgets.dataSourceSelection, {
          MSG_EMPTY: parent._msg("message.emptystatistics"),
          MSG_ERROR: parent._msg("message.emptystatistics"),
          selectionMode: "single"
        });
        parent.widgets.dataTableSelection.subscribe("rowMouseoverEvent", parent.widgets.dataTableSelection.onEventHighlightRow);
        parent.widgets.dataTableSelection.subscribe("rowMouseoutEvent", parent.widgets.dataTableSelection.onEventUnhighlightRow);
        parent.widgets.dataTableSelection.subscribe("rowClickEvent", parent.widgets.dataTableSelection.onEventSelectRow);
        parent.widgets.dataTableSelection.subscribe("rowClickEvent", parent.SelectStatisticsRow);
      },
      _setupDataStatsTables: function() {
        var renderCellTitle = function(cell, record, column, data) {
          var href = Alfresco.constants.URL_PAGECONTEXT + Alfresco.constants.URI_TEMPLATES.sitedashboardpage;
          href = href.replace("//","/").replace("{site}", data);
          cell.innerHTML += "<a href='" + href + "' target='_blank'>" + data +"</a>";
        };
        var renderCellRole = function(cell, record, column, data) {
            cell.innerHTML = $html(data);
          };
        var renderCellShortName = function(cell, record, column, data) {
          var href = Alfresco.constants.URL_PAGECONTEXT + Alfresco.constants.URI_TEMPLATES.sitedashboardpage;
          href = href.replace("//","/").replace("{site}", data);
          cell.innerHTML += "<a href='" + href + "' target='_blank'>" + data +"</a>";
        };
        var renderCellFullName = function(cell, record, column, data) {
          var href = Alfresco.constants.URL_PAGECONTEXT + Alfresco.constants.URI_TEMPLATES.userprofilepage;
          href = href.replace("//","/").replace("{userid}", data);
          cell.innerHTML += "<a href='" + href + "' target='_blank'>" + data +"</a>";
        };
        var renderCellUserName = function(cell, record, column, data) {
          var href = Alfresco.constants.URL_PAGECONTEXT + Alfresco.constants.URI_TEMPLATES.userprofilepage;
          href = href.replace("//","/").replace("{userid}", data);
          cell.innerHTML += "<a href='" + href + "' target='_blank'>" + data +"</a>";
        };
        var renderCellUserEmail = function(cell, record, column, data) {
          cell.innerHTML = $html(data);
        };

        var columnDefinitions = [{
          key: "siteTitle",
          label: parent._msg("label.siteTitle"),
          sortable: true,
          formatter: renderCellTitle,
          width: 110
        },{
          key: "siteShortName",
          label: parent._msg("label.shortName"),
          sortable: true,
          formatter: renderCellShortName
        },{
          key: "role",
          label: parent._msg("label.role"),
          sortable: true,
          formatter: renderCellRole
        },{
          key: "fullName",
          label: parent._msg("label.fullName"),
          sortable: true,
          formatter: renderCellFullName
        }, {
          key: "userId",
          label: parent._msg("label.userName"),
          sortable: true,
          formatter: renderCellUserName
        }, {
          key: "email",
          label: parent._msg("label.userEmail"),
          sortable: true,
          formatter: renderCellUserEmail
        }];
        parent.widgets.dataTableSite = new YAHOO.widget.DataTable(parent.id + "-statistics-sites-list", columnDefinitions, parent.widgets.dataSourceSite, {
          MSG_EMPTY: parent._msg("message.empty"),
          MSG_ERROR: parent._msg("message.empty")
        });
      }
    });
    new ListPanelHandler();
    return this;
  };
  YAHOO.extend(RL.UserEmailStatisticsConsole, Alfresco.ConsoleTool, {
    /**
     * Fired by YUI when parent element is available for scripting.
     * Component initialisation, including instantiation of YUI widgets and
     * event listener binding.
     * 
     * @method onReady
     */
    onReady: function() {
      var self = this;
      // Call super-class onReady() method
      RL.UserEmailStatisticsConsole.superclass.onReady.call(this);
    },
    onRefreshClick: function() {
      this.LoadUserActivity();
    },
    /**
     * Gets a custom message
     * 
     * @method _msg
     * @param messageId
     *            {string} The messageId to retrieve
     * @return {string} The custom message
     * @private
     */
    _msg: function(messageId) {
      return Alfresco.util.message.call(this, messageId, "RL.UserEmailStatisticsConsole", Array.prototype.slice.call(arguments).slice(1));
    },
    /**
     * Resets the YUI DataTable errors to our custom messages
     * 
     * NOTE: Scope could be YAHOO.widget.DataTable, so can't use "this"
     * 
     * @method _setDefaultDataTableErrors
     * @param dataTable
     *            {object} Instance of the DataTable
     */
    _setDefaultDataTableErrors: function(dataTable) {
      var msg = Alfresco.util.message;
      dataTable.set("MSG_EMPTY", msg("message.empty", "RL.UserEmailStatisticsConsole"));
      dataTable.set("MSG_ERROR", msg("message.error", "RL.UserEmailStatisticsConsole"));
    }
  });
})();