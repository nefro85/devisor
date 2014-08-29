$( document ).ready(function() {
 
    $( "<a/>", {
			html: "List",
			target: "_blank",
			href: "list.do"
	}).appendTo("#root");
	
	$("<br>").appendTo("#root");
	
	$( "<a/>", {
			html: "4mPower Log",
			target: "_blank",
			href: "log4mpower.do"
	}).appendTo("#root");
	
	$("<br>").appendTo("#root");
	
	$( "<a/>", {
			html: "package list",
			target: "_blank",
			href: "package_list.do"
	}).appendTo("#root");
	
	$("<br>").appendTo("#root");
	
	$( "<a/>", {
			html: "exec sql",			
			href: "#",
			click: function () {
				$("<div id='run_sql'> RUN SQL </div>").appendTo("#root");
				$("<input name='package'>").appendTo("#run_sql");
				$("<input name='sql'>").appendTo("#run_sql");
				$( "<a/>", {
					html: "fire sql",
					target: "_blank",
					href: "#",
					click: function () {
							url = "exec_sql.do?package=" + $("input[name='package']").val() + "&sql=" + encodeURIComponent ( $("input[name='sql']").val());
							window.open(url);
						}
					
					}).appendTo("#run_sql");
			}
	}).appendTo("#root");
				
 
});