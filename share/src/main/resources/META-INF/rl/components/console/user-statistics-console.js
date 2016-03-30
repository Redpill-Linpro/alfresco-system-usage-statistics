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
 * @class RL.UserStatisticsConsole
 */
(function() {
  /**
   * YUI Library aliases
   */
  var Dom = YAHOO.util.Dom;
  var $html = Alfresco.util.encodeHTML;
  var Bubbling = YAHOO.Bubbling;
  /**
   * UserStatisticsConsole constructor.
   * 
   * @param {String}
   *            htmlId The HTML id of the parent element
   * @return {RL.UserStatisticsConsole} The new UserStatisticsConsole instance
   * @constructor
   */
  RL.UserStatisticsConsole = function(htmlId) {
    this.name = "RL.UserStatisticsConsole";
    RL.UserStatisticsConsole.superclass.constructor.call(this, htmlId);
    /* Register this component */
    Alfresco.util.ComponentManager.register(this);
    /* Load YUI Components */
    Alfresco.util.YUILoaderHelper.require(["button", "container", "datasource", "datatable", "paginator", "json", "history"], this.onComponentsLoaded, this);
    /* Define panel handlers */
    var parent = this;
    this.data = {
      "selectionData": new Object(),
      "internalData": new Object(),
      "externalData": new Object()
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
      //YAHOO.util.Event.addListener(selectBox, "onchange", function (e) { this.OnChangeYearFilter(e); } );
      //http://localhost:8081/share/proxy/alfresco/slingshot/doclib/treenode/node/alfresco/company/home/Data%20Dictionary/Redpill-Linpro/Statistics/UserStatistics
      this.timerShowLoadingMessage = YAHOO.lang.later(2000, this, this.fnShowLoadingMessage);
      Alfresco.util.Ajax.request({
        url: Alfresco.constants.PROXY_URI + "slingshot/doclib/treenode/node/alfresco/company/home/Data%20Dictionary/Redpill-Linpro/Statistics/UserStatistics",
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
        url: Alfresco.constants.PROXY_URI + "/slingshot/doclib2/doclist/all/node/alfresco/company/home/Data%20Dictionary/Redpill-Linpro/Statistics/UserStatistics/" + target + "?sortField=cm%3acreated&sortAsc=false",
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
            // data payload that will be returned to the callback function
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
      //http://localhost:8080/alfresco/service/api/node/workspace/SpacesStore/1/content
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
        this.data.internalData.users = response.json.internal;
        this.data.internalData.startIndex = 0;
        this.data.internalData.totalRecords = response.json.internal.length;
        this.data.externalData.users = response.json.external;
        this.data.externalData.startIndex = 0;
        this.data.externalData.totalRecords = response.json.external.length;
        var successHandlerInternal = function(request, response, payload) {
          parent.widgets.dataTableInternal.onDataReturnInitializeTable.call(parent.widgets.dataTableInternal, request, response, payload);
        };
        var successHandlerExternal = function(request, response, payload) {
          parent.widgets.dataTableExternal.onDataReturnInitializeTable.call(parent.widgets.dataTableExternal, request, response, payload);
        };
        var oCallbackInternal = {
          success: successHandlerInternal,
          failure: successHandlerInternal,
          scope: this.widgets.dataTableInternal,
          argument: this.widgets.dataTableInternal.getState()
        };
        this.widgets.dataSourceInternal.sendRequest("", oCallbackInternal);
        var oCallbackExternal = {
          success: successHandlerExternal,
          failure: successHandlerExternal,
          scope: this.widgets.dataTableExternal,
          argument: this.widgets.dataTableExternal.getState()
        };
        this.widgets.dataSourceExternal.sendRequest("", oCallbackExternal);
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
        parent.widgets.dataSourceInternal = new YAHOO.util.FunctionDataSource(function() {
          return YAHOO.lang.JSON.stringify(parent.data.internalData);
        }, {
          "responseType": YAHOO.util.FunctionDataSource.TYPE_JSON,
          responseSchema: {
            resultsList: "users",
            metaFields: {
              recordOffset: "startIndex",
              totalRecords: "totalRecords"
            }
          }
        });
        parent.widgets.dataSourceExternal = new YAHOO.util.FunctionDataSource(function() {
          return YAHOO.lang.JSON.stringify(parent.data.externalData);
        }, {
          "responseType": YAHOO.util.FunctionDataSource.TYPE_JSON,
          responseSchema: {
            resultsList: "users",
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
        var renderCellUserName = function(cell, record, column, data) {
          cell.innerHTML = $html(data);
        };
        var renderCellFullName = function(cell, record, column, data) {
          cell.innerHTML = $html(data);
        };
        var renderCellLogins = function(cell, record, column, data) {
          cell.innerHTML = $html(data);
        };
        var renderCellLastActivity = function(cell, record, column, data) {
          if (data == "") {
            data = Alfresco.util.message("statistics-console.noactivity", "RL.UserStatisticsConsole");
          }
          cell.innerHTML = $html(data);
        };
        var columnDefinitions = [{
          key: "userName",
          label: parent._msg("label.userName"),
          sortable: true,
          formatter: renderCellUserName
        }, {
          key: "fullName",
          label: parent._msg("label.fullName"),
          sortable: true,
          formatter: renderCellFullName
        }, {
          key: "logins",
          label: parent._msg("label.logins"),
          sortable: true,
          formatter: renderCellLogins
        }, {
          key: "lastActivity",
          label: parent._msg("label.lastActivity"),
          sortable: true,
          formatter: renderCellLastActivity
        }];
        parent.widgets.dataTableInternal = new YAHOO.widget.DataTable(parent.id + "-statistics-internal-users-list", columnDefinitions, parent.widgets.dataSourceInternal, {
          MSG_EMPTY: parent._msg("message.empty"),
          MSG_ERROR: parent._msg("message.empty")
        });
        parent.widgets.dataTableExternal = new YAHOO.widget.DataTable(parent.id + "-statistics-external-users-list", columnDefinitions, parent.widgets.dataSourceExternal, {
          MSG_EMPTY: parent._msg("message.empty"),
          MSG_ERROR: parent._msg("message.empty")
        });
      }
    });
    new ListPanelHandler();
    return this;
  };
  YAHOO.extend(RL.UserStatisticsConsole, Alfresco.ConsoleTool, {
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
      RL.UserStatisticsConsole.superclass.onReady.call(this);
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
      return Alfresco.util.message.call(this, messageId, "RL.UserStatisticsConsole", Array.prototype.slice.call(arguments).slice(1));
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
      dataTable.set("MSG_EMPTY", msg("message.empty", "RL.UserStatisticsConsole"));
      dataTable.set("MSG_ERROR", msg("message.error", "RL.UserStatisticsConsole"));
    }
  });
})();