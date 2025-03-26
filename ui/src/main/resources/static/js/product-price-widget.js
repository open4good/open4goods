/**
 * Initializes the price widget including chart and table for a given product.
 * @param {Object} options - Configuration options for the widget.
 * @param {string} options.widgetPrefix - Prefix used for generating element IDs.
 * @param {Array} options.priceHistory - Array of price history objects with properties: timestamp, price.
 * @param {Array} options.events - Array of commercial event objects.
 * @param {string} options.canvasId - ID of the canvas element where the chart will be rendered.
 * @param {string} options.tableId - ID of the table element for offers.
 * @param {Object} options.periodButtons - Object mapping period keys ('15days', '3months', '6months', 'max') to button element IDs.
 * @param {string} [options.defaultPeriod='3months'] - The default period to display.
 * @param {Object} [options.chartColors] - Optional color configuration for the chart.
 */
function initPriceWidget(options) {
    // Set default values for optional parameters
    options.defaultPeriod = options.defaultPeriod || '3months';
    options.chartColors = options.chartColors || {
        backgroundColor: '#4cb1ff',
        borderColor: '#41a3ef'
    };

    // Prepare price history data
    const rawPriceHistoryData = options.priceHistory || [];
    const priceHistory = rawPriceHistoryData.length > 0
        ? rawPriceHistoryData.map(({ timestamp, price }) => ({ x: timestamp, y: price }))
        : [];

    const commercialEvents = options.events || [];

    // Build annotations for Chart.js annotation plugin based on commercial events
    const annotations = {};
    commercialEvents.forEach((event, index) => {
        const boxBgColor = 'rgba(0, 0, 0, 0.1)';
        const boxBorderColor = 'rgba(0, 0, 0, 0.6)';
        const commonLabel = {
            display: true,
            content: event.label,
            backgroundColor: 'rgba(255,255,255,0.7)',
            color: '#000',
            font: {
                size: 10,
                weight: 'bold'
            },
            padding: 4,
            borderRadius: 4
        };

        if (event.startDate === event.endDate) {
            annotations['event' + index] = {
                type: 'line',
                mode: 'vertical',
                scaleID: 'x',
                value: event.startDate,
                borderColor: boxBorderColor,
                borderWidth: 2,
                label: {
                    ...commonLabel,
                    position: 'start'
                }
            };
        } else {
            annotations['event' + index] = {
                type: 'box',
                xMin: event.startDate,
                xMax: event.endDate,
                backgroundColor: boxBgColor,
                borderColor: boxBorderColor,
                borderWidth: 1,
                label: {
                    ...commonLabel,
                    position: 'start'
                }
            };
        }
    });

    // Define date ranges based on current date
    const today = new Date();
    const dateRanges = {
        '15days': new Date(today.getTime() - (15 * 24 * 60 * 60 * 1000)),
        '3months': new Date(today.getFullYear(), today.getMonth() - 3, today.getDate()),
        '6months': new Date(today.getFullYear(), today.getMonth() - 6, today.getDate()),
        'max': null // All data
    };

    const timeUnitMapping = {
        '15days': 'day',
        '3months': 'week',
        '6months': 'week',
        'max': 'month'
    };

    // Helper functions
    function filterDataByDate(data, startDate) {
        return data.filter(item => new Date(item.x) >= startDate);
    }
    function getFilteredData(data, period) {
        if (dateRanges[period]) {
            return filterDataByDate(data, dateRanges[period]);
        }
        return data;
    }
    function hasDataInPeriod(data, period) {
        return getFilteredData(data, period).length > 0;
    }
    function findMinValue(data) {
        return data.reduce((min, p) => p.y < min ? p.y : min, data[0].y);
    }
    function findMaxValue(data) {
        return data.reduce((max, p) => p.y > max ? p.y : max, data[0].y);
    }
    function setActiveButton(activeButton) {
        // This selector assumes the widget is wrapped in an element with an id like "new-price-widget" (adjust as needed)
        const buttonGroupSelector = `#${options.widgetPrefix}-price-widget .btn-group button`;
        const buttons = document.querySelectorAll(buttonGroupSelector);
        buttons.forEach(button => {
            button.classList.remove('btn-primary', 'active-period-btn');
            button.classList.add('btn-secondary');
            button.style.opacity = '0.5';
        });
        if (activeButton) {
            activeButton.classList.remove('btn-secondary');
            activeButton.classList.add('btn-primary', 'active-period-btn');
            activeButton.style.opacity = '1';
        }
    }
    function updateChart(chart, data, period) {
        const filteredData = getFilteredData(data, period);
        // Force a new array reference so changes are detected
        chart.data.datasets[0].data = filteredData.slice();
        // Reset x-axis scale before applying new values
        chart.options.scales.x.min = undefined;
        chart.options.scales.x.max = undefined;
        chart.options.scales.x.time.unit = timeUnitMapping[period] || 'month';

        if (filteredData.length > 0) {
            const newMin = findMinValue(filteredData);
            const newMax = findMaxValue(filteredData);
            chart.options.scales.y.suggestedMin = Math.floor(newMin * 0.98);
            chart.options.scales.y.suggestedMax = Math.ceil(newMax * 1.02);
            chart.options.scales.x.min = filteredData[0].x;
            chart.options.scales.x.max = filteredData[filteredData.length - 1].x;
        } else {
            chart.options.scales.y.suggestedMin = undefined;
            chart.options.scales.y.suggestedMax = undefined;
        }
        // Call update without the 'none' parameter to force a full update
        chart.update();
    }

    // Define the initialization function
    function init() {
        // Initialize Bootstrap tooltips for accessibility
        var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });

        if (priceHistory.length > 0) {
            let defaultPeriod = options.defaultPeriod;
            if (!hasDataInPeriod(priceHistory, defaultPeriod)) {
                defaultPeriod = 'max';
            }
            const initialFiltered = getFilteredData(priceHistory, defaultPeriod);
            let xMinVal, xMaxVal, yMinVal, yMaxVal;
            if (initialFiltered.length > 0) {
                xMinVal = initialFiltered[0].x;
                xMaxVal = initialFiltered[initialFiltered.length - 1].x;
                yMinVal = findMinValue(initialFiltered);
                yMaxVal = findMaxValue(initialFiltered);
            }
            const canvasElem = document.getElementById(options.canvasId);
            const ctx = canvasElem.getContext('2d');
            const chart = new Chart(ctx, {
                type: 'line',
                data: {
                    datasets: [{
                        label: 'Prix Neuf',
                        data: initialFiltered.slice(),
                        fill: true,
                        backgroundColor: options.chartColors.backgroundColor,
                        borderColor: options.chartColors.borderColor,
                        borderWidth: 2,
                        tension: 0,
                        pointRadius: 0,
                        pointHoverRadius: 6,
                        pointHitRadius: 10
                    }]
                },
                options: {
                    interaction: {
                        mode: 'nearest',
                        intersect: false
                    },
                    scales: {
                        x: {
                            type: 'time',
                            time: {
                                unit: timeUnitMapping[defaultPeriod] || 'month',
                                displayFormats: {
                                    day: 'dd MMM yy',
                                    week: 'dd MMM yy',
                                    month: 'MMM yy'
                                },
                                tooltipFormat: 'dd MMM yyyy'
                            },
                            min: xMinVal,
                            max: xMaxVal,
                            title: {
                                display: true,
                                text: 'Date'
                            }
                        },
                        y: {
                            suggestedMin: yMinVal ? Math.floor(yMinVal * 0.98) : undefined,
                            suggestedMax: yMaxVal ? Math.ceil(yMaxVal * 1.02) : undefined,
                            title: {
                                display: true,
                                text: 'Prix (€)'
                            }
                        }
                    },
                    plugins: {
                        legend: {
                            display: false
                        },
                        annotation: {
                            clip: false,
                            annotations: annotations
                        }
                    }
                }
            });

            chart.update();
            const defaultButton = document.getElementById(options.periodButtons[defaultPeriod]);
            setActiveButton(defaultButton);

            // Attach event listeners to period buttons
            Object.keys(options.periodButtons).forEach(period => {
                const button = document.getElementById(options.periodButtons[period]);
                if (button) {
                    if (!hasDataInPeriod(priceHistory, period)) {
                        button.disabled = true;
                        button.classList.add('disabled');
                        button.style.opacity = '0.3';
                    } else {
                        button.addEventListener('click', function () {
                            updateChart(chart, priceHistory, period);
                            setActiveButton(this);
                        });
                    }
                }
            });
        }
    }
    
    // Run the initialization either immediately or on DOMContentLoaded
    if (document.readyState === "loading") {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
}
