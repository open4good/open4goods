	// NAVBAR SEARCH 


$(document).ready(function() {
	// Product search input
	if ($('#navbarSearchForm')) {		
		$('#navbarSearchForm').on("change",function (e) {
	    	$('#navbarSearchForm').attr("action","/recherche/"+encodeURIComponent($('#navbar-search').val()));
	    });
	}
} );


	// UI RANGE SLIDER 

	var rangetimers = {};
	
	
    if (d.querySelector('.input-slider-range-container')) {
        [].slice.call(d.querySelectorAll('.input-slider-range-container')).map(function (el) {
			 var c = el,
				// low = d.getElementById("input-slider-range-value-low"),
				// e = d.getElementById("input-slider-range-value-high"),
				
				low = c.querySelector(":scope .value-low"),
				e = c.querySelector(":scope .value-high"),
				
				f = [d, e];

			noUiSlider.create(c, {
				start: [parseInt(low.getAttribute('data-range-value-low')), parseInt(e.getAttribute('data-range-value-high'))],
				connect: !0,
				tooltips: true,
				step: 1,
				range: {
					min: parseInt(c.getAttribute('data-range-value-min')),
					max: parseInt(c.getAttribute('data-range-value-max'))
				}
			}), c.noUiSlider.on("update", function (a, b) {
				f[b].textContent = a[b]
				el.setAttribute("data-range-value-min",a[0]);
				el.setAttribute("data-range-value-max",a[1]);
				
				// delaying event sending
				id=c.getAttribute('id');
				clearTimeout(rangetimers[id]);
				rangetimers[id] = setTimeout(function() {
					
					
					event = new CustomEvent(
						"change", 
						{
							detail: {
								id: id,
								min: a[0],
								max: a[1]
							},
							bubbles: true,
							cancelable: true
						}
					);
					
					c.dispatchEvent(event);
					
				}, 300);
				
			});
        });
    }


