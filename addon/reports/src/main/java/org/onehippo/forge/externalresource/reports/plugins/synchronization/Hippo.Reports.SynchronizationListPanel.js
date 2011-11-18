Ext.ns('Hippo.Reports');

Hippo.Reports.SynchronizationListPanel = Ext.extend(Hippo.Reports.Portlet, {

    constructor: function(config) {
        var self = this;

        this.store = config.store;
        this.pageSize = config.pageSize;
        this.paging = config.paging;
        this.pagingText = config.resources['documents-paging'];
        this.noDataText = config.noDataText;
        this.autoExpandColumn = config.autoExpandColumn;
        this.syncActions = config.syncActions;
        this.columns = config.columns;

        var columnCount = this.columns.length;

        for (var i = 0; i < columnCount; i++) {
            //console.log('id:' + this.columns[i].id);
            if (this.columns[i].id == 'syncActions') {
                this.columns[i].xtype = 'actioncolumn';
                this.columns[i].width = (self.syncActions.length * 20) + 4;
                var sActions = new Array();
                for (var j = 0; j < self.syncActions.length; j++) {
                    sActions.push({
                        icon: self.syncActions[j].icon,
                        tooltip: self.syncActions[j].name,

                        handler: (function(j) {
                            return function(grid, rowIndex, colIndex) {
                                //console.log(grid.getView().getCell(rowIndex, colIndex));
                                self.performSyncAction(grid, rowIndex, '' + self.syncActions[j].name);
                            }
                        })(j),
                        getClass: function(v, meta, rec) {
                            var itemState = rec.get("synchState");
                            meta.css = itemState == "Synchronized" ? "checkEnabled" :
                                     itemState == "Unsynchronized" ? "updateEnabled" : "deleteEnabled";
                        }
                    });
                }
                this.columns[i].items = sActions;
            }
        }
        var grid = new Ext.grid.GridPanel({
            store: self.store,
            colModel: new Ext.grid.ColumnModel({
                defaults: {
                    menuDisabled: true,
                    sortable: false
                },
                columns: self.columns
            }),
            loadMask: false,
            autoExpandColumn: self.autoExpandColumn,
            border: false,
            disableSelection: true,
            viewConfig: {
                scrollOffset: 2
            },
            bbar: self.paging ? new Ext.PagingToolbar({
                pageSize: self.pageSize,
                store: self.store,
                displayInfo: true,
                beforePageText: 'Page',
                displayMsg: self.pagingText,
                emptyMsg: '',
                afterPageText: '',
                listeners: {
                    afterrender: function(bbar) {
                        bbar.last.hideParent = true;
                        bbar.last.hide();
                        bbar.refresh.hideParent = true;
                        bbar.refresh.hide();
                    }
                }
            }) : null,

            /*tbar: [
             {
             //ref: '../primaryBtn',
             iconCls: 'icon-total-refresh',
             text: 'Refresh',
             disabled: false,
             handler: function() {
             self.performMassSync();
             alert('test3');
             }
             }
             ],*/

            listeners: {
                cellclick: function(grid, rowIndex, columnIndex, event) {
                    var record = grid.getStore().getAt(rowIndex);
                    var columnId = grid.getColumnModel().getColumnId(columnIndex);
                    //console.log(record.data);
                    if (columnId != 'syncActions') {
                        self.fireEvent('documentSelected', {path: record.data.path});
                    }

                }
            }
        });

        this.performSyncAction = function(grid, rowIndex, medium) {
            var record = grid.getStore().getAt(rowIndex);
            self.fireEvent('performSyncAction', {path: record.data.path, medium: medium});
        }

        this.performMassSync = function() {
            self.fireEvent('performMassSync');
        }

        config = Ext.apply(config, {
            bodyCssClass: 'hippo-reports-document-list',
            items:[ grid ]
        });

        Hippo.Reports.SynchronizationListPanel.superclass.constructor.call(this, config);

    },

    loadStore: function() {
        this.store.load({
            params: {
                start: 0,
                limit: this.pageSize
            }
        });
    },

    checkNoData: function(component) {
        if (this.store.getTotalCount() == 0) {
            this.showMessage(this.noDataText);
        }
    },

    initComponent: function() {
        Hippo.Reports.SynchronizationListPanel.superclass.initComponent.call(this);
        this.store.on('load', this.checkNoData, this);
        Hippo.Reports.RefreshObservableInstance.addListener("refresh", this.loadStore, this);
        this.loadStore();
    },

    destroy: function() {
        Hippo.Reports.SynchronizationListPanel.superclass.destroy.call(this);
        Hippo.Reports.RefreshObservableInstance.removeListener("refresh", this.loadStore, this);
    }

});

Ext.reg('Hippo.Reports.SynchronizationListPanel', Hippo.Reports.SynchronizationListPanel);
