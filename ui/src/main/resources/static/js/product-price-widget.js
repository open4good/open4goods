/* product-price-widget.js */

// Function to initialize DataTable
function initTable(tableId) {
    // Initialize DataTable with specified options
    tableNew = $(tableId).DataTable({
        searching: false,
        paging: false,
        info: false,
        select: 'single',
        "order": [ [2, 'asc'] ]
    });
}

/**
 * Utility function to parse your date string if needed.
 * If your timestamp is already "YYYY-MM-DD", new Date(...) usually works fine.
 * Adjust as necessary if you have time components or different formats.
 */
function parseDate(str) {
    // Example: handle "YYYY-MM-DD" or "YYYY-MM-DD HH:mm:ss"
    // If you have "YYYY-MM-DD HH:mm:ss", you might do:
    // return new Date(str.replace(' ', 'T'));
    return new Date(str);
}

/**
 * Builds annotations for Chart.js annotation plugin from an array of events.
 * Each event has: { startDate, endDate, label } as Date objects + a string label.
 */
function buildAnnotations(events) {
    const annotations = {};
    events.forEach((ev, i) => {
        // Basic styling
        const boxBgColor = 'rgba(0, 0, 0, 0.1)';
        const boxBorderColor = 'rgba(0, 0, 0, 0.6)';
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

        if (ev.startDate.getTime() === ev.endDate.getTime()) {
            // Single-day event => vertical line
            annotations['event_' + i] = {
                type: 'line',
                mode: 'vertical',
                scaleID: 'x',
                value: ev.startDate,
                borderColor: boxBorderColor,
                borderWidth: 2,
                label: {
                    ...commonLabel,
                    position: 'start'
                }
            };
        } else {
            // Multi-day event => box annotation
            annotations['event_' + i] = {
                type: 'box',
                xMin: ev.startDate,
                xMax: ev.endDate,
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
        // If there's 0 or 1 data points, you can remove everything or keep only "max"
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

    // 2) Parse events
    const rawEvents = options.events || [];
    const commercialEvents = rawEvents.map(ev => ({
        startDate: parseDate(ev.startDate),
        endDate: parseDate(ev.endDate),
        label: ev.label
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

    // 5) Update chart function with event remapping (Option B)
    function updateChart(chart, period) {
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

            // ---- Handle commercial events ignoring the year ----
            const minDate = filteredData[0].x;
            const maxDate = filteredData[filteredData.length - 1].x;

            function extractMonthDay(date) {
                return date.getMonth() * 100 + date.getDate(); // e.g., March 5 -> 305
            }

            // 1) Filter by month/day only
            const minMD = extractMonthDay(minDate);
            const maxMD = extractMonthDay(maxDate);

            const visibleEvents = commercialEvents.filter(ev => {
                const startMD = extractMonthDay(ev.startDate);
                const endMD   = extractMonthDay(ev.endDate);

                if (minMD <= maxMD) {
                    // Normal case: e.g. March → May
                    return endMD >= minMD && startMD <= maxMD;
                } else {
                    // Cross-year case: e.g. November → February
                    return endMD >= minMD || startMD <= maxMD;
                }
            });

            // 2) Remap each event’s month/day into the chart’s actual year(s)
            function remapEventToChartYear(ev, minDate, maxDate) {
                const minYear = minDate.getFullYear();
                const maxYear = maxDate.getFullYear();

                const minMD = extractMonthDay(minDate);
                const maxMD = extractMonthDay(maxDate);

                const startMD = extractMonthDay(ev.startDate);
                const endMD   = extractMonthDay(ev.endDate);

                if (minYear === maxYear) {
                    // Single-year chart range
                    return {
                        startDate: new Date(minYear, ev.startDate.getMonth(), ev.startDate.getDate()),
                        endDate:   new Date(minYear, ev.endDate.getMonth(),   ev.endDate.getDate()),
                        label: ev.label
                    };
                }

                // Cross-year chart range (assume maxYear = minYear + 1)
                let newStartYear = (startMD >= minMD) ? minYear : maxYear;
                let newEndYear   = (endMD   >= minMD) ? minYear : maxYear;

                let newStartDate = new Date(newStartYear, ev.startDate.getMonth(), ev.startDate.getDate());
                let newEndDate   = new Date(newEndYear,   ev.endDate.getMonth(),   ev.endDate.getDate());

                // Ensure start <= end
                if (newStartDate > newEndDate) {
                    const temp = newStartDate;
                    newStartDate = newEndDate;
                    newEndDate = temp;
                }
                return {
                    startDate: newStartDate,
                    endDate:   newEndDate,
                    label: ev.label
                };
            }

            const mappedEvents = visibleEvents.map(ev => remapEventToChartYear(ev, minDate, maxDate));

            // 3) Build the annotation config
            chart.options.plugins.annotation.annotations = buildAnnotations(mappedEvents);
        } else {
            // No data => clear or handle gracefully
            chart.options.scales.x.min = undefined;
            chart.options.scales.x.max = undefined;
            chart.options.scales.y.suggestedMin = undefined;
            chart.options.scales.y.suggestedMax = undefined;
            chart.options.plugins.annotation.annotations = {};
        }

        // Adjust time unit
        chart.options.scales.x.time.unit = timeUnitMapping[period] || 'month';
        chart.update();
    }

    // 6) Initialize the chart (if we have data)
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
                        // Initially build annotations from the raw events.
                        // They will be corrected immediately after via updateChart().
                        annotations: buildAnnotations(commercialEvents)
                    }
                }
            }
        });

        // 7) Hook up period buttons
        Object.keys(options.periodButtons).forEach(periodKey => {
            const btnId = options.periodButtons[periodKey];
            const button = document.getElementById(btnId);
            if (!button) return;

            // Make it "active" if it's our default
            if (periodKey === currentPeriod) {
                setActiveButton(button);
            }

            button.addEventListener('click', () => {
                updateChart(myChart, periodKey);
                setActiveButton(button);
            });
        });

        // IMPORTANT: Call updateChart once on initial load
        updateChart(myChart, currentPeriod);
    }

    // 8) Simple helper to style the active button
    function setActiveButton(activeBtn) {
        const allBtns = document.querySelectorAll('.period-group .period-btn');
        allBtns.forEach(b => {
            b.classList.remove('btn-primary', 'active-period-btn');
            b.classList.add('btn-secondary');
            b.style.opacity = '0.5';
        });
        activeBtn.classList.remove('btn-secondary');
        activeBtn.classList.add('btn-primary', 'active-period-btn');
        activeBtn.style.opacity = '1';
    }

    // 9) Initialize Bootstrap tooltips (if using them)
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
