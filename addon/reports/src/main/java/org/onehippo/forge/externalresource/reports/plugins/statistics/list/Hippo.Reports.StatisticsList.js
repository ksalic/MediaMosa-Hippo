Ext.ns('Hippo.Reports');

Hippo.Reports.StatisticsListPanel = Ext.extend(Hippo.Reports.Portlet, {

    constructor: function(config) {
        var self = this;

        this.store = config.store;
        this.pageSize = config.pageSize;
        this.paging = config.paging;
        this.pagingText = config.resources['items-paging'];
        this.noDataText = config.noDataText;
        this.autoExpandColumn = config.autoExpandColumn;

        this.columns = config.columns;



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
            enableColumnMove: false,
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

            listeners: {
                cellclick: function(grid, rowIndex, columnIndex, event) {
                    var record = grid.getStore().getAt(rowIndex);
                    var serialized = "";
                    for(var j in record.data){
                        serialized = serialized + j + "=" + record.data[j] + "|";
                    }
                    self.fireEvent('rowSelected', {rowData: serialized});
                }
            }

        });


        config = Ext.apply(config, {
            bodyCssClass: 'hippo-reports-statistics-list',
            items:[ grid ]
        });

        Hippo.Reports.StatisticsListPanel.superclass.constructor.call(this, config);

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
        Hippo.Reports.StatisticsListPanel.superclass.initComponent.call(this);
        this.store.on('load', this.checkNoData, this);
        Hippo.Reports.RefreshObservableInstance.addListener("refresh", this.loadStore, this);
        this.loadStore();
    },

    destroy: function() {
        Hippo.Reports.StatisticsListPanel.superclass.destroy.call(this);
        Hippo.Reports.RefreshObservableInstance.removeListener("refresh", this.loadStore, this);
    }

});

Ext.reg('Hippo.Reports.StatisticsListPanel', Hippo.Reports.StatisticsListPanel);
