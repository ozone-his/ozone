from superset.security import SupersetSecurityManager
import logging
from flask_appbuilder.security.views import AuthOAuthView
from flask_appbuilder.baseviews import expose
import os
from six.moves.urllib.parse import urlencode
import redis
import time
from flask import (
    redirect,
    request,
    g
)

TOKEN_PREFIX = "oauth_id_token_"
REDIS_HOST = os.getenv("REDIS_HOST", "redis")
REDIS_PORT = os.getenv("REDIS_PORT", 6379)
redis_db = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)

class CustomAuthOAuthView(AuthOAuthView):

    @expose("/logout/")
    def logout(self, provider="keycloak", register=None):
        user_id = str(g.user.id)
        provider_obj = self.appbuilder.sm.oauth_remotes[provider]
        redirect_url = request.url_root.strip('/') + self.appbuilder.get_url_for_login
        logout_base_url = provider_obj.api_base_url + "logout"
        params = {
            "client_id": provider_obj.client_id,
            "post_logout_redirect_uri": redirect_url
        }
        if redis_db.get(TOKEN_PREFIX + user_id):
            params["id_token_hint"] = redis_db.get(TOKEN_PREFIX + user_id)

        ret = super().logout()
        time.sleep(1)

        return redirect("{}?{}".format(logout_base_url, urlencode(params)))


class CustomSecurityManager(SupersetSecurityManager):
    # override the logout function
    authoauthview = CustomAuthOAuthView

    def oauth_user_info(self, provider, response=None):
        logging.debug("Oauth2 provider: {0}.".format(provider))
        if provider == 'keycloak':
            me = self.appbuilder.sm.oauth_remotes[provider].get('userinfo').json()
            return {
                "username": me.get("preferred_username", ""),
                "first_name": me.get("given_name", ""),
                "last_name": me.get("family_name", ""),
                "email": me.get("email", ""),
                'roles': me.get('roles', ['Public']),
                'id_token': response["id_token"]
            }
        return {}

    def auth_user_oauth(self, userinfo):
        user = super(CustomSecurityManager, self).auth_user_oauth(userinfo)
        redis_db.set(TOKEN_PREFIX + str(user.id), userinfo["id_token"])
        del userinfo["id_token"]
        roles = [self.find_role(x) for x in userinfo['roles']]
        roles = [x for x in roles if x is not None]
        user.roles = roles
        self.update_user(user)
        return user
