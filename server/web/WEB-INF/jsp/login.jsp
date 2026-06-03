<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Service</title>
    <link rel="shortcut icon" href="${servletPath}/favicon.ico" type="image/x-icon">
    <link rel="icon" href="${servletPath}/favicon.ico" type="image/x-icon">
    
    <link rel="stylesheet" href="${servletPath}/css/login.css" />
    <link rel="stylesheet" href="${servletPath}/css/custom-alert.css" />
    <script src="${servletPath}/js/common/jquery-3.2.1.min.js"></script>
    <script src="${servletPath}/js/common/custom-alert.js" charset="UTF-8"></script>
    <script src="${servletPath}/js/common_util.js"></script>
    <script>var servletPath = '${servletPath}';</script>
    <script src="${servletPath}/js/login.js"></script>
</head>
<body>
<div class="login-popup" id="loginPopup">
    <div class="login-popup_img"><img src="${servletPath}/images/Logo_LTEX.png" /></div>
    <div class="login-popup-inputGroup">
        <div class="login-popup_idInput"><input type="text" placeholder="ID" id="idInput" value="etriadmin" readonly></div>
        <div class="login-popup_pwInput"><input type="password" placeholder="Password" id="passwordInput"></div>
    </div>
    <div class="login-popup-buttonGroup">
        <div class="login-popup_loginBtn"><button type="button" id="loginBtn">Login</button></div>
        <div class="login-popup_signinBtn"><button type="button" id="signinBtn">Sign up</button></div>
    </div>
</div>

<div class="signup-popup hidden" id="signupPopup">
    <div class="signup-popup_closeBtn">X</div>
    <div class="signup-popup_img"><img src="${servletPath}/images/Logo_LTEX.png" /></div>
    <div class="signup-popup-inputGroup">
        <div class="signup-popup_idInput"><input type="text" placeholder="ID" id="signupIdInput"></div>
        <div class="signup-popup_pwInput"><input type="password" placeholder="Password" id="signupPasswordInput"></div>
        <div class="signup-popup_pwConfirmInput"><input type="password" placeholder="Confirm Password" id="signupPasswordConfirmInput"></div>
        <div class="signup-popup_nameInput"><input type="text" placeholder="Name" id="signupNameInput"></div>
    </div>
    <div class="signup-popup-buttonGroup">
        <div class="signup-popup_signupBtn"><button type="button" id="signupBtn">Sign up</button></div>
    </div>
</div>
<div class="overlay hidden" id="overlay"></div>
</body>
</html>
