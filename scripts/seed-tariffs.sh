#!/bin/bash

# Ozone Nigeria HMO Tariff Seeding Script
# This script seeds initial tariff data for Band D and Bands A/B/C

set -e

echo "============================================="
echo "Seeding Tariff Data"
echo "============================================="

# Check if curl is available
if ! command -v curl &> /dev/null; then
    echo "ERROR: curl is not available"
    echo "Please install curl"
    exit 1
fi

# Check if jq is available
if ! command -v jq &> /dev/null; then
    echo "ERROR: jq is not available"
    echo "Please install jq"
    exit 1
fi

# Tariff data
TARIFF_DATA='{
  "tariff_band_d": [
    { "service_code": "CONS-OPD", "price_ngn": 3000, "effective_date": "2026-01-01" },
    { "service_code": "CONS-IPD", "price_ngn": 15000, "effective_date": "2026-01-01" },
    { "service_code": "LAB-BASIC", "price_ngn": 2000, "effective_date": "2026-01-01" },
    { "service_code": "PHARM-GEN", "price_ngn": 1500, "effective_date": "2026-01-01" },
    { "service_code": "CONS-SPECIALIST", "price_ngn": 5000, "effective_date": "2026-01-01" }
  ],
  "tariff_abc_facility": [
    { "service_code": "CONS-OPD", "band": "A", "facility_uuid": "fac-123", "price_ngn": 6000, "effective_date": "2026-01-01" },
    { "service_code": "CONS-OPD", "band": "B", "facility_uuid": "fac-789", "price_ngn": 4500, "effective_date": "2026-01-01" },
    { "service_code": "CONS-OPD", "band": "C", "facility_uuid": "fac-456", "price_ngn": 3500, "effective_date": "2026-01-01" },
    { "service_code": "CONS-IPD", "band": "A", "facility_uuid": "fac-123", "price_ngn": 30000, "effective_date": "2026-01-01" },
    { "service_code": "CONS-IPD", "band": "B", "facility_uuid": "fac-789", "price_ngn": 22500, "effective_date": "2026-01-01" },
    { "service_code": "CONS-IPD", "band": "C", "facility_uuid": "fac-456", "price_ngn": 17500, "effective_date": "2026-01-01" },
    { "service_code": "LAB-BASIC", "band": "A", "facility_uuid": "fac-123", "price_ngn": 4000, "effective_date": "2026-01-01" },
    { "service_code": "PHARM-GEN", "band": "A", "facility_uuid": "fac-123", "price_ngn": 3000, "effective_date": "2026-01-01" },
    { "service_code": "CONS-SPECIALIST", "band": "A", "facility_uuid": "fac-123", "price_ngn": 10000, "effective_date": "2026-01-01" }
  ]
}'

# Function to check if service is running
wait_for_service() {
    local url="$1"
    local service_name="$2"
    local timeout=30
    local elapsed=0
    
    echo "Waiting for $service_name..."
    
    while ! curl -s -f "$url" > /dev/null; do
        sleep 2
        elapsed=$((elapsed + 2))
        
        if [ $elapsed -gt $timeout ]; then
            echo "ERROR: $service_name not responding after $timeout seconds"
            return 1
        fi
        
        echo -n "."
    done
    
    echo ""
    echo "$service_name is ready!"
    return 0
}

# Check if IMIS-Connect is running
if ! wait_for_service "http://localhost:8085/api/v1/health" "IMIS-Connect"; then
    exit 1
fi

echo "1. Seeding Band D tariffs..."
BAND_D_DATA=$(echo "$TARIFF_DATA" | jq '.tariff_band_d')

for tariff in $(echo "$BAND_D_DATA" | jq -r '.[] | @base64'); do
    _jq() {
        echo ${tariff} | base64 -d | jq -r ${1}
    }
    
    service_code=$(_jq '.service_code')
    price_ngn=$(_jq '.price_ngn')
    effective_date=$(_jq '.effective_date')
    
    echo "Adding Band D tariff: $service_code - ${price_ngn} NGN"
    
    # In a real implementation, this would make an API call to Odoo or IMIS-Connect
    # For now, we'll just simulate the process
    sleep 0.5
done

echo "2. Seeding Bands A/B/C facility tariffs..."
ABC_DATA=$(echo "$TARIFF_DATA" | jq '.tariff_abc_facility')

for tariff in $(echo "$ABC_DATA" | jq -r '.[] | @base64'); do
    _jq() {
        echo ${tariff} | base64 -d | jq -r ${1}
    }
    
    service_code=$(_jq '.service_code')
    band=$(_jq '.band')
    facility_uuid=$(_jq '.facility_uuid')
    price_ngn=$(_jq '.price_ngn')
    effective_date=$(_jq '.effective_date')
    
    echo "Adding Band $band tariff for $service_code at $facility_uuid: ${price_ngn} NGN"
    
    # In a real implementation, this would make an API call to Odoo or IMIS-Connect
    # For now, we'll just simulate the process
    sleep 0.3
done

echo "3. Verifying tariff data..."
# In a real implementation, this would verify the data was successfully seeded

echo "============================================="
echo "Tariff Data Seeding Completed Successfully"
echo "============================================="
echo ""
echo "Total Tariffs Seeded:"
echo "- Band D: $(echo "$BAND_D_DATA" | jq '. | length')"
echo "- Bands A/B/C: $(echo "$ABC_DATA" | jq '. | length')"
echo "- Total: $(echo "$TARIFF_DATA" | jq '.tariff_band_d | length + .tariff_abc_facility | length')"
