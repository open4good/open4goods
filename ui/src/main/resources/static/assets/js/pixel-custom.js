// NAVBAR SEARCH 


$(document).ready(function() {
    // Product search input
    if ($('#navbarSearchForm')) {
        $('#navbarSearchForm').on("change", function(e) {
            $('#navbarSearchForm').attr("action", "/recherche/" + encodeURIComponent($('#navbar-search').val()));
        });
    }
});

// UI RANGE SLIDER

var rangetimers = {};


/**
 * This is hacky, but works for the actual ranges and avoid specific ranges configuration
 */
function getRange(lowValue, highValue) {


    if (highValue < 1000) {
        return {
            min: parseInt(lowValue),
            max: parseInt(highValue)
        };
    } else {
        return {
            'min': [lowValue],             // Start at 0
            '50%': [1000, 10],       // Fine control up to 500
            '80%': [1500, 100],     // Larger steps starting from 1500
            '70%': [5000, 500],     // Wider steps from 5000 onwards
            '80%': [10000, 1000],   // Even larger steps from 10000
            'max': [highValue]          // Final maximum value
        };

    }

}if (d.querySelector('.input-slider-range-container')) {
    [].slice.call(d.querySelectorAll('.input-slider-range-container')).map(function(el) {
        var id = el.getAttribute("id");

        // Retrieve existing input elements using Thymeleaf-generated IDs
        var lowInput = document.getElementById("slider-min-" + id);
        var highInput = document.getElementById("slider-max-" + id);

        var lowValue = parseInt(lowInput.getAttribute('value'));
        var highValue = parseInt(highInput.getAttribute('value'));

        noUiSlider.create(el, {
            start: [lowValue, highValue],
            connect: true,
            tooltips: true,
            range: {
                'min': lowValue,
                'max': highValue
            },
            step: 1
        });

        // Update slider when inputs are changed
        lowInput.addEventListener('change', function () {
            el.noUiSlider.set([this.value, null]);
        });

        highInput.addEventListener('change', function () {
            el.noUiSlider.set([null, this.value]);
        });

        // Update inputs when slider values change
        el.noUiSlider.on("update", function(a, b) {
            if (b === 0) {
                lowInput.value = a[0];
            } else {
                highInput.value = a[1];
            }

            // Set data attributes for min/max range values
            el.setAttribute("data-range-value-min", a[0]);
            el.setAttribute("data-range-value-max", a[1]);

            // Delaying event sending
            clearTimeout(rangetimers[id]);
            rangetimers[id] = setTimeout(function() {
                var event = new CustomEvent("change", {
                    detail: {
                        id: id,
                        min: a[0],
                        max: a[1]
                    },
                    bubbles: true,
                    cancelable: true
                });
                el.dispatchEvent(event);
            }, 300);
        });
    });
}

