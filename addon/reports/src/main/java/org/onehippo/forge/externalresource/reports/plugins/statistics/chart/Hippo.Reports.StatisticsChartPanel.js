(function(Ext) {
    Ext.ns('Hippo.Reports');

    // Fix for ExtJs issue OPEN-663: labelRenderer function is lost after hide/show
    // (see http://www.sencha.com/forum/showthread.php?93003-OPEN-663-Chart-loses-labelRenderer-after-hide-show)
    // Workaround: do not delete the labelRenderer
    Ext.override(Ext.chart.CartesianChart, {
        createAxis : function(axis, value) {
            var o = Ext.apply({}, value),
                    ref,
                    old;

            if (this[axis]) {
                old = this[axis].labelFunction;
                this.removeFnProxy(old);
                this.labelFn.remove(old);
            }
            if (o.labelRenderer) {
                ref = this.getFunctionRef(o.labelRenderer);
                o.labelFunction = this.createFnProxy(function(v) {
                    return ref.fn.call(ref.scope, v);
                });
                // delete o.labelRenderer;
                this.labelFn.push(o.labelFunction);
            }
            if (axis.indexOf('xAxis') > -1 && o.position == 'left') {
                o.position = 'bottom';
            }
            return o;
        }
    });

    Hippo.Reports.StatisticsChartPanel = Ext.extend(Hippo.Reports.Portlet, {
        chart : null,
        noData : null,

        constructor : function(config) {
            var self = this;

            Ext.chart.Chart.CHART_URL = config.chartsFlashPath;
            this.legendPosition = config.legendPosition;
            this.noDataText = config.noDataText;

            if (config.chartType == "column") {
                this.chart = new Hippo.Reports.Chart.ColumnChart({
                    store: config.store,
                    xField: 'name',
                    yField: 'total',
                    xAxis: new Ext.chart.CategoryAxis({
                        title : ' ' + config.xAxisTitle + ' ',
                        labelRenderer: function(value) {
                            if (value.indexOf('__hidden__') == 0) {
                                return '';
                            } else {
                                return value;
                            }
                        }
                    }),
                    yAxis: new Ext.chart.NumericAxis({
                        title : config.yAxisTitle,
                        minorUnit: -1,
                        labelRenderer: function(value) {
                            // only show the value if it is an integer
                            if (!isNaN(value) && (parseFloat(value) == parseInt(value))) {
                                return value;
                            } else {
                                return '';
                            }
                        }
                    }),
                    //extra styles get applied to the chart defaults
                    extraStyle: {
                        yAxis: {
                            titleRotation : -90
                        },
                        padding : 0
                    }
                });
            } else if (config.chartType == "bar") {
                this.chart = new Hippo.Reports.Chart.BarChart({
                    store: config.store,
                    xField: 'total',
                    yField: 'name',
                    xAxis: new Ext.chart.NumericAxis({
                        title : config.xAxisTitle,
                        minorUnit: -1,
                        labelRenderer: function(value) {
                            // only show the value if it is an integer
                            if (!isNaN(value) && (parseFloat(value) == parseInt(value))) {
                                return value;
                            } else {
                                return '';
                            }
                        }
                    }),
                    yAxis: new Ext.chart.CategoryAxis({
                        title : config.yAxisTitle,
                        labelRenderer: function(value) {
                            if (value.indexOf('__hidden__') == 0) {
                                return '';
                            } else {
                                return value;
                            }
                        }
                    }),
                    extraStyle: {
                        yAxis: {
                            titleRotation : -90
                        },
                        padding : 0
                    }
                });
            } else {
                this.chart = new Hippo.Reports.Chart.PieChart({
                    store: config.store,
                    dataField: 'total',
                    categoryField: 'name',
                    //extra styles get applied to the chart defaults
                    extraStyle: {
                        legend: {
                            display: config.legendPosition,
                            padding: 1,
                            font: {
                                family: 'Tahoma',
                                size: 11
                            }
                        }
                    }
                });
            }

            this.chart.on("beforerefresh", function() {
                if (this.noData === true) {
                    return false;
                }
            }, this);

            config = Ext.apply(config, { items : this.chart });
            Hippo.Reports.StatisticsChartPanel.superclass.constructor.call(this, config);
        },

        loadStore: function() {
            this.store.load();
        },

        initComponent : function() {
            Hippo.Reports.StatisticsChartPanel.superclass.initComponent.call(this);
            this.store.on('load', this.checkNoData, this);
            Hippo.Reports.RefreshObservableInstance.addListener("refresh", this.loadStore, this);
            this.chart.on("swfReallyReady", this.loadStore, this);
        },

        destroy: function() {
            Hippo.Reports.StatisticsChartPanel.superclass.destroy.call(this);
            Hippo.Reports.RefreshObservableInstance.removeListener("refresh", this.loadStore, this);
        },

        checkNoData: function(store, records, options) {
            if (this.noDataText != null && this.noDataText != '') {
                this.noData = true;
                for (var i in records) {
                    if (typeof records[i].data != "undefined" && records[i].data.total > 0) {
                        this.noData = false;
                        break;
                    }
                }
                if (this.noData) {
                    this.showMessage(this.noDataText);
                }
            }
        }

    });

    Ext.reg('Hippo.Reports.StatisticsChartPanel', Hippo.Reports.StatisticsChartPanel);
})(Ext);