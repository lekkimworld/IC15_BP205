<html>
<body>
<p>
You have now authorized this application for OAuth 2.0 so it's able to access data in IBM Connections 
on your behalf. The user object we created for you is listed below.
<br/>
Key: <%= ((com.ontimesuite.gc.labs.bp205.User)request.getAttribute("User")).getKey() %>
<br/>
GUID: <%= ((com.ontimesuite.gc.labs.bp205.User)request.getAttribute("User")).getGUID() %>
<br/>
UID: <%= ((com.ontimesuite.gc.labs.bp205.User)request.getAttribute("User")).getUid() %>
<br/>
Email: <%= ((com.ontimesuite.gc.labs.bp205.User)request.getAttribute("User")).getEmail() %>
</p>
<p>
You can now return to IBM Connections and reload the widget.
</p>
</body>
</html>
