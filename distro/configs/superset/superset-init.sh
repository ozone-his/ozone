#!/usr/bin/env bash
STEP_CNT=6

echo_step() {
cat <<EOF
######################################################################
Init Step ${1}/${STEP_CNT} [${2}] -- ${3}
######################################################################
EOF
}
# Initialize the database
echo_step "1" "Starting" "Applying DB migrations"
superset db upgrade
echo_step "1" "Complete" "Applying DB migrations"

# Create an admin user
echo_step "2" "Starting" "Setting up admin user ( admin / $ADMIN_PASSWORD )"
superset fab create-admin \
              --username admin \
              --firstname Superset \
              --lastname Admin \
              --email admin@superset.com \
              --password $ADMIN_PASSWORD
echo_step "2" "Complete" "Setting up admin user"
# Create default roles and permissions
echo_step "3" "Starting" "Setting up roles and perms"
superset init

echo_step "3" "Complete" "Setting up roles and perms"
if [ "$SUPERSET_LOAD_EXAMPLES" = "yes" ]; then
    # Load some data to play with" row_number() over(partition by visit.patient_id order by visit.visit_id) as number_occurences," +
    echo_step "4" "Starting" "Loading examples"
    # If Cypress run which consumes superset_test_config – load required data for tests
    if [ "$CYPRESS_CONFIG" == "true" ]; then
        superset load_test_users
        superset load_examples --load-test-data
    else
        superset load_examples
    fi
    echo_step "4" "Complete" "Loading examples"
fi
echo_step "5" "Complete" "Loading datasources"
superset import-datasources -p /etc/superset/datasources/datasources.yaml
echo_step "5" "Complete" "Loading datasources"
echo_step "6" "Complete" "Updating datasources"
superset set_database_uri -d $ANALYTICS_DATASOURCE_NAME -u postgresql://$ANALYTICS_DB_USER:$ANALYTICS_DB_PASSWORD@$ANALYTICS_DB_HOST:5432/$ANALYTICS_DB_NAME
echo_step "6" "Complete" "Updating datasources"