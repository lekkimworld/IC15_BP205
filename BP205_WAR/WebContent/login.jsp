<html lang="en"
      xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <title>Login Form</title>
    </head>
    <body>
        <h2>Hello, please log in:</h2>
        <form method="post" action="j_security_check">
            <table columns="2" role="presentation"> 
                <tr>
                    <td>Please type your user name</td>
                    <td><input name="j_username" autocomplete="off" size="20" /></td>
                </tr>
                <tr>
                    <td>Please type your password:</td>
                    <td><input name="j_password" autocomplete="off" size="20"/></td>
                </tr>
            </table>
            <p>
                <input type="submit" value="Submit"/>
                &nbsp;
                <input type="reset" value="Reset"/>
            </p>
        </form>
    </body>
</html>
