/**
 * Constructor.
 * 
 */
var BP205Widget = function() {
	
};
BP205Widget.prototype.ACTION_ACCEPTED = "accepted";
BP205Widget.prototype.ACTION_REJECTED = "rejected";
BP205Widget.prototype.ACTION_UNHANDLED = "unhandled";

/**
 * onLoad method.
 * 
 */
BP205Widget.prototype.onLoad = function() {
	
};

/**
 * onView method.
 */
BP205Widget.prototype.onView = function() {
	// build ui
	this._createUI(this.ACTION_UNHANDLED);
};

BP205Widget.prototype._createUI = function(action) {
	var rewritten = this.iContext.io.rewriteURI("http://inside.intravision.dk/bp205/api/events/" + action);
	this._get(rewritten, action, function(json) {
		if ("ERROR" == json.Status) {
			// this is an error
			this._error.apply(this, arguments);
		} else {
			// success - render ui
			this._events.apply(this, arguments);
		}
	});
}

/**
 * Called when events are received back from the API.
 * 
 * @param json
 */
BP205Widget.prototype._events = function() {
	var root = this.iContext.getRootElement();
	root.innerHTML = "Building events...";

	var that = this;
	var result = "";
	var json = arguments[0];
	var currentAction = arguments[1];
	result += "<p>Events that are <b>" + currentAction + "</b></p>";
	for (var idx in json.Events) {
		// get event
		var ev = json.Events[idx];

		// convert dates
		var startDate = ev.StartDT.ontimeISOToJSDate();
		var endDate = ev.EndDT.ontimeISOToJSDate();
		
		// build ui
		result += "<p>";
		result += "<a href=\"" + ev.URL + "\" target=\"_top\">" + ev.Subject;
		result += "</a><br/>";
		if (ev.Location) result += "Location: " + ev.Location + "<br/>";
		result += startDate.ontimeFormat() + " to " + endDate.ontimeFormat() + "<br/>";
		result += "From community <a href=\"" + ev.Community.URL + "\">" + ev.Community.Name + "</a><br/>";
		if (currentAction == this.ACTION_REJECTED || currentAction == this.ACTION_UNHANDLED) {
			// add accept link
			result += "<a href=\"javascript:void(0)\" class=\"bp205action\" eventId=\"" + 
				ev.ID + "\" eventAction=\"accept\">accept</a>";
			result += " | ";
		}
		if (currentAction == this.ACTION_ACCEPTED || currentAction == this.ACTION_UNHANDLED) {
			// add reject link
			result += "<a href=\"javascript:void(0)\" class=\"bp205action\" eventId=\"" + 
				ev.ID + "\" eventAction=\"reject\">reject</a>";
			result += " | ";
		}
		if (currentAction != this.ACTION_UNHANDLED) {
			// add unhandle link
			result += "<a href=\"javascript:void(0)\" class=\"bp205action\" eventId=\"" + 
				ev.ID + "\" eventAction=\"unhandle\">unhandle</a>";
		}
		result += "</p>";
	}
	result += "<hr/>";
	result += "<p>";
	if (currentAction != this.ACTION_UNHANDLED) {
		result += "| <a href=\"javascript:void(0)\" id=\"bp205action_unhandled\">unhandled</a>";
	}
	if (currentAction != this.ACTION_ACCEPTED) {
		result += " | <a href=\"javascript:void(0)\" id=\"bp205action_accepted\">accepted</a>";	
	}
	if (currentAction != this.ACTION_REJECTED) {
		result += " | <a href=\"javascript:void(0)\" id=\"bp205action_rejected\">rejected</a>";
	}
	result += " |";
	root.innerHTML = result;

	// add event handlers to links to change ui
	var actions = [this.ACTION_UNHANDLED, this.ACTION_ACCEPTED, this.ACTION_REJECTED];
	for (var idx in actions) {
		var action = actions[idx];
		if (dojo.byId("bp205action_" + action)) {
			dojo.byId("bp205action_" + action).onclick = (function(action) {
				return dojo.hitch(that, function() {
					that._createUI(action);
				})
			})(action);
		}
	}

	// add event handler to links to accept/reject events
	dojo.forEach(dojo.query(".bp205action"), function(link) {
		link.onclick = function() {
			this["_" + link.getAttribute("eventAction")](link.getAttribute("eventId"), currentAction);
		}.bind(that);
	});
};

BP205Widget.prototype._reject = function(eventId, action) {
	this._post("/bp205/api/event/reject", {"EventID": eventId}, action, function() {
		this._createUI(action);
	});
}

BP205Widget.prototype._accept = function(eventId, action) {
	this._post("/bp205/api/event/accept", {"EventID": eventId}, action, function() {
		this._createUI(action);
	});
}

BP205Widget.prototype._unhandle = function(eventId, action) {
	this._post("/bp205/api/event/unhandle", {"EventID": eventId}, action, function() {
		this._createUI(action);
	});
}

/**
 * Called when an error is received back from the API.
 * 
 * @param json
 */
BP205Widget.prototype._error = function(json) {
	if ("OAuthMissing" == json.ErrorCode) {
		// we are missing OAuth info for this user - show ui
		this._errorOAuthMissing(json.OAuth.URL);
	} else {
		this._errorGeneric(json);
	}
};

BP205Widget.prototype._errorOAuthMissing = function(authUrl) {
	var root = this.iContext.getRootElement();
	root.innerHTML = "You have not authorized this application to use OAuth 2.0 to access IBM Connections data on your behalf. Before you " + 
		"do so we cannot continue. Please <a href=\"" + authUrl + "\" target=\"_new\">authorize</a>.";
};

BP205Widget.prototype._errorGeneric = function(json) {
	var root = this.iContext.getRootElement();
	root.innerHTML = "Oops!! That didn't work!<br/>ErrorCode: " + json.ErrorCode + "<br/>ErrorText: " + json.ErrorText;
};

BP205Widget.prototype._get = function(url, args, callback) {
	this._url("GET", url, null, args, callback);
};
BP205Widget.prototype._post = function(url, data, args, callback) {
	this._url("POST", url, data, args, callback);
};
BP205Widget.prototype._url = function(method, url, data, args, callback) {
	// keep context
	var that = this;

	// do the request
	var payload = {
		"url": url, 
		"handleAs": "text",
		"failOk": true, 
		"handle": function(data, xhr) {
			var result = null;
			var status = xhr.xhr.status;
			try {
				// parse
				result = eval("(" + (data && data.hasOwnProperty("responseText") ? data.responseText : data) + ")");

			} catch (e) {
				// unable to parse
				result = {
					"Status" : "ERROR",
					"ErrorCode": "json error (" + e + ")",
					"ErrorText": data
				};
			}

			// create array with args if any
			var callbackArgs = [];
			callbackArgs.push(result);
			if (args) {
				if (args instanceof Array) {
					for (var idx in args) {
						callbackArgs.push(args[idx]);
					}
				} else {
					callbackArgs.push(args);
				}
			}

			// callback
			callback.apply(that, callbackArgs);
		}
	};
	if (method == "GET") {
		dojo.xhrGet(payload);
	} else if (method == "POST") {
		if (data) {
			payload.postData = JSON.stringify(data);
		}
		dojo.xhrPost(payload);
	}
};

String.prototype.ontimeISOToJSDate = function() {
	var y = parseInt(this.substr(0, 4), 10);
	var m = parseInt(this.substr(5, 2), 10);
	var d = parseInt(this.substr(8, 2), 10);
	var h = parseInt(this.substr(11, 2), 10);
	var n = parseInt(this.substr(14, 2), 10);
	var s = parseInt(this.substr(17, 2), 10);
	var result = new Date();
	result.setUTCFullYear(y, m - 1, d);
	result.setUTCHours(h, n, s, 0);
	return result;
};

Date.prototype.ontimeFormat = function() {
	return this.getFullYear() + "-" + 
		(this.getMonth()+1) + "-" + this.getDate() 
		+ " " + this.getHours() + ":" + ("0" + this.getMinutes()).substring(-2);
};
