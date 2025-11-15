/* product-price-widget.js */

// Function to initialize DataTable
function initTable(tableId) {
    // Initialize DataTable with specified options
    tableNew = $(tableId).DataTable({
        searching: false,
        paging: false,
        info: false,
        select: 'single',
        "order": [[2, 'asc']]
    });
}

/**
 * Utility function to parse your date string (price data).
 * If your timestamp is "YYYY-MM-DD", new Date(...) works fine.
 */
function parseDate(str) {
    // If needed, handle time component, e.g. new Date(str.replace(' ', 'T'));
    return new Date(str);
}

/**
 * Parse an event date ignoring the year. Return {month, day} (0-based month).
 */
function parseEventMonthDay(str) {
    // str is "YYYY-MM-DD"
    const parts = str.split('-');
    // parts[0] = YYYY, parts[1] = MM, parts[2] = DD
    return {
        month: parseInt(parts[1], 10) - 1, // zero-based
        day:   parseInt(parts[2], 10)
    };
}

/**
 * Build Chart.js annotations from an array of "mapped" events
 * (where startDate and endDate are actual Date objects).
 */
function buildAnnotations(events) {
    const annotations = {};

    // Create vertical offsets for labels to avoid collisions
    const yOffsets = [0, 15, 30, -15, -30];

    events.forEach((ev, i) => {
        // Use the event’s color or a fallback
        const defaultBgColor = 'rgba(39, 245, 41, 0.5)'; // box fill
        const defaultBorderColor = 'rgba(0, 0, 0, 0.3)';
        const eventColor = ev.color || defaultBgColor;

        // Single day => line, multi-day => box?
        const isSingleDay = ev.startDate.getTime() === ev.endDate.getTime();

        // Common label settings
        const commonLabel = {
            display: true,
            content: ev.label,
            backgroundColor: 'rgba(255,255,255,0.8)',
            color: '#000',
            font: {
                size: 10,
                weight: 'bold'
            },
            padding: 4,
            borderRadius: 4
        };

        if (isSingleDay) {
            // Single-day event => vertical line
            annotations['event_' + i] = {
                type: 'line',
                mode: 'vertical',
                scaleID: 'x',
                value: ev.startDate,
                borderColor: eventColor || defaultBorderColor,
                borderWidth: 2,
                label: {
                    ...commonLabel,
                    position: 'start', // bottom of chart area
                    yAdjust: yOffsets[i % yOffsets.length] 
                }
            };
        } else {
            // Multi-day event => box
            annotations['event_' + i] = {
                type: 'box',
                xMin: ev.startDate,
                xMax: ev.endDate,
                backgroundColor: eventColor,
                borderColor: defaultBorderColor,
                borderWidth: 1,
                label: {
                    ...commonLabel,
                    position: 'start',
                    yAdjust: yOffsets[i % yOffsets.length]
                }
            };
        }
    });

    return annotations;
}

/**
 * Filter the chart data by the chosen period.
 * dateRanges: an object that maps '15days', '3months', '6months', 'max' -> actual Date or null
 */
function filterDataByDateRange(data, startDate) {
    if (!startDate) return data; // "max" => no filtering
    return data.filter(item => item.x >= startDate);
}

/**
 * Remove period buttons if your dataset does not cover those periods.
 */
function removePeriodButtonsIfNotEnoughData(priceHistory, periodButtons) {
    if (priceHistory.length < 2) {
        // If there are 0 or 1 data points, remove everything except "max"
        Object.keys(periodButtons).forEach(period => {
            if (period !== 'max') {
                const btn = document.getElementById(periodButtons[period]);
                btn && btn.remove();
            }
        });
        return;
    }

    // Sort data ascending by date
    priceHistory.sort((a, b) => a.x - b.x);
    const earliest = priceHistory[0].x;
    const latest = priceHistory[priceHistory.length - 1].x;
    const diffDays = (latest - earliest) / (1000 * 3600 * 24);

    if (diffDays < 15) {
        ['3months', '6months', 'max'].forEach(p => {
            const btn = document.getElementById(periodButtons[p]);
            btn && btn.remove();
        });
        return;
    }
    if (diffDays < 90) {
        ['6months', 'max'].forEach(p => {
            const btn = document.getElementById(periodButtons[p]);
            btn && btn.remove();
        });
        return;
    }
    if (diffDays < 180) {
        const btn = document.getElementById(periodButtons['max']);
        btn && btn.remove();
    }
}

/**
 * Main chart update function. Re-filters data and rebuilds the annotation config.
 */
function updateChart(chart, period, priceHistory, commercialEvents, dateRanges, timeUnitMapping) {
    const startDate = dateRanges[period] || null;
    const filteredData = filterDataByDateRange(priceHistory, startDate);

    chart.data.datasets[0].data = filteredData.slice();

    if (filteredData.length > 0) {
        chart.options.scales.x.min = filteredData[0].x;
        chart.options.scales.x.max = filteredData[filteredData.length - 1].x;

        const minVal = filteredData.reduce((acc, p) => Math.min(acc, p.y), filteredData[0].y);
        const maxVal = filteredData.reduce((acc, p) => Math.max(acc, p.y), filteredData[0].y);
        chart.options.scales.y.suggestedMin = Math.floor(minVal * 0.98);
        chart.options.scales.y.suggestedMax = Math.ceil(maxVal * 1.02);

        // Compute the year range in the filtered data
        const startYear = filteredData[0].x.getFullYear();
        const endYear = filteredData[filteredData.length - 1].x.getFullYear();

        let mappedEvents = [];
        // Map each commercial event to every year in the range
        for (let y = startYear; y <= endYear; y++) {
            commercialEvents.forEach(ev => {
                mappedEvents.push({
                    startDate: new Date(y, ev.startMonthDay.month, ev.startMonthDay.day),
                    endDate:   new Date(y, ev.endMonthDay.month, ev.endMonthDay.day),
                    label: ev.label,
                    color: ev.color
                });
            });
        }

        // Filter events to only those that intersect with the filteredData range (unless period is "max")
        const finalEvents = period === "max" ? mappedEvents : mappedEvents.filter(e => {
            return e.endDate >= filteredData[0].x && e.startDate <= filteredData[filteredData.length - 1].x;
        });

        chart.options.plugins.annotation.annotations = buildAnnotations(finalEvents);
    } else {
        // No data => clear or handle gracefully
        chart.options.scales.x.min = undefined;
        chart.options.scales.x.max = undefined;
        chart.options.scales.y.suggestedMin = undefined;
        chart.options.scales.y.suggestedMax = undefined;
        chart.options.plugins.annotation.annotations = {};
    }

    // Adjust time unit for x-axis
    chart.options.scales.x.time.unit = timeUnitMapping[period] || 'month';
    chart.update();
}

/**
 * initPriceWidget: Main function to initialize the chart + period buttons + events.
 */
function initPriceWidget(options) {
    initTable('#' + options.tableId);

    // Set defaults if not provided
    options.defaultPeriod = options.defaultPeriod || '3months';
    options.chartColors = options.chartColors || {
        backgroundColor: '#4cb1ff',
        borderColor: '#41a3ef'
    };

    // 1) Parse priceHistory
    const rawPH = options.priceHistory || [];
    const priceHistory = rawPH.map(ph => ({
        x: parseDate(ph.timestamp),
        y: ph.price
    })).sort((a, b) => a.x - b.x);

    // 2) Parse events ignoring year
    const rawEvents = options.events || [];
    // Store month/day plus the label & color
    const commercialEvents = rawEvents.map(ev => ({
        startMonthDay: parseEventMonthDay(ev.startDate),
        endMonthDay: parseEventMonthDay(ev.endDate),
        label: ev.label,
        color: ev.color // e.g. "rgba(176,255,76,0.25)"
    }));

    // 3) Define dateRanges for each period
    const now = new Date();
    const dateRanges = {
        '15days': new Date(now.getTime() - 15 * 24 * 60 * 60 * 1000),
        '3months': new Date(now.getFullYear(), now.getMonth() - 3, now.getDate()),
        '6months': new Date(now.getFullYear(), now.getMonth() - 6, now.getDate()),
        'max': null
    };

    // Decide the time unit for Chart.js
    const timeUnitMapping = {
        '15days': 'day',
        '3months': 'week',
        '6months': 'week',
        'max': 'month'
    };

    // 4) Remove period buttons if not enough data
    removePeriodButtonsIfNotEnoughData(priceHistory, options.periodButtons);

    // 5) Initialize the chart (if we have data)
    function initChart() {
        if (priceHistory.length === 0) {
            // No data => you might hide the chart container or show a "No data" message
            return;
        }

        // Determine the default period
        let currentPeriod = options.defaultPeriod;
        // Prepare initial filtered data
        const startDate = dateRanges[currentPeriod] || null;
        const initialData = filterDataByDateRange(priceHistory, startDate);

        let xMinVal, xMaxVal, yMinVal, yMaxVal;
        if (initialData.length > 0) {
            xMinVal = initialData[0].x;
            xMaxVal = initialData[initialData.length - 1].x;
            yMinVal = initialData.reduce((acc, p) => Math.min(acc, p.y), initialData[0].y);
            yMaxVal = initialData.reduce((acc, p) => Math.max(acc, p.y), initialData[0].y);
        }

        const ctx = document.getElementById(options.canvasId).getContext('2d');
        const myChart = new Chart(ctx, {
            type: 'line',
            data: {
                datasets: [{
                    label: 'Prix',
                    data: initialData,
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
                            unit: timeUnitMapping[currentPeriod] || 'month',
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
                        // Start with empty. We'll fill it after we know the chart’s date range:
                        annotations: {}
                    }
                }
            }
        });

        // 6) Hook up period buttons
        Object.keys(options.periodButtons).forEach(periodKey => {
            const btnId = options.periodButtons[periodKey];
            const button = document.getElementById(btnId);
            if (!button) return;

            // Mark it as active if it matches the default period
            if (periodKey === currentPeriod) {
                setActiveButton(button);
            }

            button.addEventListener('click', () => {
                updateChart(myChart, periodKey, priceHistory, commercialEvents, dateRanges, timeUnitMapping);
                setActiveButton(button);
            });
        });

        // Call updateChart once on initial load
        updateChart(myChart, currentPeriod, priceHistory, commercialEvents, dateRanges, timeUnitMapping);

        // Force a final update on the next frame to ensure annotations are correctly positioned
        requestAnimationFrame(() => {
            myChart.update();
        });
    }

    // Simple helper to style the active period button
    function setActiveButton(activeBtn) {
        const allBtns = document.querySelectorAll('.period-group .period-btn-' + options.widgetPrefix);
        allBtns.forEach(b => {
            b.classList.remove('btn-primary', 'active-period-btn');
            b.classList.add('btn-secondary');
            b.style.opacity = '0.5';
        });
        activeBtn.classList.remove('btn-secondary');
        activeBtn.classList.add('btn-primary', 'active-period-btn');
        activeBtn.style.opacity = '1';
    }

    // Initialize Bootstrap tooltips (if used)
    function initTooltips() {
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(el => new bootstrap.Tooltip(el));
    }

    // Run everything
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            initTooltips();
            initChart();
        });
    } else {
        initTooltips();
        initChart();
    }
}
