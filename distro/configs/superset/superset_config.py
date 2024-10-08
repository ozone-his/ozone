import logging
import os
from dotenv import load_dotenv
from cachelib import RedisCache

from cachelib.file import FileSystemCache
logger = logging.getLogger()

def password_from_env(url):
    return os.getenv("ANALYTICS_DB_PASSWORD")

SQLALCHEMY_CUSTOM_PASSWORD_STORE = password_from_env

def get_env_variable(var_name, default=None):
    """Get the environment variable or raise exception."""
    try:
        return os.environ[var_name]
    except KeyError:
        if default is not None:
            return default
        else:
            error_msg = "The environment variable {} was missing, abort...".format(
                var_name
            )
            raise EnvironmentError(error_msg)


MAPBOX_API_KEY = os.getenv('MAPBOX_API_KEY', '')

DATABASE_DIALECT = get_env_variable("DATABASE_DIALECT", "postgres")
DATABASE_USER = get_env_variable("DATABASE_USER", "superset")
DATABASE_PASSWORD = get_env_variable("DATABASE_PASSWORD", "superset")
DATABASE_HOST = get_env_variable("DATABASE_HOST", "postgres")
DATABASE_PORT = get_env_variable("DATABASE_PORT", 5432)
DATABASE_DB = get_env_variable("DATABASE_DB", "superset")

SQLALCHEMY_TRACK_MODIFICATIONS = get_env_variable("SQLALCHEMY_TRACK_MODIFICATIONS", True)
SECRET_KEY = get_env_variable("SECRET_KEY", 'thisISaSECRET_1234')

# The SQLAlchemy connection string.
SQLALCHEMY_DATABASE_URI = "%s://%s:%s@%s:%s/%s" % (
    DATABASE_DIALECT,
    DATABASE_USER,
    DATABASE_PASSWORD,
    DATABASE_HOST,
    DATABASE_PORT,
    DATABASE_DB,
)

REDIS_HOST = get_env_variable("REDIS_HOST", "redis")
REDIS_PORT = get_env_variable("REDIS_PORT", 6379)
REDIS_CELERY_DB = get_env_variable("REDIS_CELERY_DB", 0)
REDIS_RESULTS_DB = get_env_variable("REDIS_CELERY_DB", 1)

RESULTS_BACKEND = RedisCache(host=REDIS_HOST, port=REDIS_PORT, key_prefix='superset_results')
# RESULTS_BACKEND = FileSystemCache("/app/superset_home/sqllab")

class CeleryConfig(object):
    BROKER_URL = f"redis://{REDIS_HOST}:{REDIS_PORT}/{REDIS_CELERY_DB}"
    CELERY_IMPORTS = ("superset.sql_lab",)
    CELERY_RESULT_BACKEND = f"redis://{REDIS_HOST}:{REDIS_PORT}/{REDIS_RESULTS_DB}"
    CELERY_ANNOTATIONS = {"tasks.add": {"rate_limit": "10/s"}}
    CELERY_TASK_PROTOCOL = 1

CACHE_CONFIG = {
    'CACHE_TYPE': 'redis',
    'CACHE_DEFAULT_TIMEOUT': 300,
    'CACHE_KEY_PREFIX': 'superset_',
    'CACHE_REDIS_HOST': 'redis',
    'CACHE_REDIS_PORT': 6379,
    'CACHE_REDIS_DB': 1,
    'CACHE_REDIS_URL': 'redis://redis:6379/1'
}

CELERY_CONFIG = CeleryConfig
SQLLAB_CTAS_NO_LIMIT = True
PERMANENT_SESSION_LIFETIME = 86400

class ReverseProxied(object):

    def __init__(self, app):
        self.app = app

    def __call__(self, environ, start_response):
        script_name = environ.get('HTTP_X_SCRIPT_NAME', '')
        if script_name:
            environ['SCRIPT_NAME'] = script_name
            path_info = environ['PATH_INFO']
            if path_info.startswith(script_name):
                environ['PATH_INFO'] = path_info[len(script_name):]

        scheme = environ.get('HTTP_X_SCHEME', '')
        if scheme:
            environ['wsgi.url_scheme'] = scheme
        return self.app(environ, start_response)


ADDITIONAL_MIDDLEWARE = [ReverseProxied, ]
ENABLE_PROXY_FIX = True

# Enable the security manager API.
FAB_ADD_SECURITY_API = True

if os.getenv("ENABLE_OAUTH") == "true":
    from flask_appbuilder.security.manager import AUTH_OAUTH
    from security import CustomSecurityManager
    AUTH_ROLES_SYNC_AT_LOGIN = True
    AUTH_USER_REGISTRATION = True
    AUTH_USER_REGISTRATION_ROLE = "Admin"
    CUSTOM_SECURITY_MANAGER = CustomSecurityManager
    LOGOUT_REDIRECT_URL = os.environ.get("SUPERSET_URL")
    AUTH_TYPE = AUTH_OAUTH
    OAUTH_PROVIDERS = [
        {
            'name': 'keycloak',
            'token_key': 'access_token',  # Name of the token in the response of access_token_url
            'icon': 'fa-key',   # Icon for the provider
            'remote_app': {
                'client_id': os.environ.get("SUPERSET_CLIENT_ID","superset"),  # Client Id (Identify Superset application)
                'client_secret': os.environ.get("SUPERSET_CLIENT_SECRET"),  # Secret for this Client Id (Identify Superset application)
                'api_base_url': os.environ.get("ISSUER_URL").rstrip('/') + "/protocol/openid-connect/",
                'client_kwargs': {
                    'scope': 'openid profile email',
                },
                'logout_redirect_uri': os.environ.get("SUPERSET_URL"),
                'server_metadata_url': os.environ.get("ISSUER_URL").rstrip('/') + '/.well-known/openid-configuration',  # URL to get metadata from
            }
        }
    ]
