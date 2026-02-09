# Ozone Nigeria HMO Integration

This project implements a comprehensive health insurance management system for Nigeria's HMO context, built on top of the Ozone HIS (Health Information System) platform. The solution integrates Ozone HIS with openIMIS (International Medical Information System) to provide band-aware insurance claims management.

## Key Features

### Band-Aware Tariff Management
- **Band D**: Single shared tariff table for all Band D hospitals
- **Bands A/B/C**: Facility-specific tariffs with identical service codes but varying prices per facility

### Insurance Integration
- **Eligibility Checks**: Real-time insurance eligibility verification
- **Claim Submission**: FHIR-based claim submission to openIMIS
- **Status Tracking**: Claim status monitoring and updates
- **Integration Gateway**: IMIS-Connect microservice for FHIR/GraphQL integration

### System Architecture
- **Ozone HIS Core**: OpenMRS, Odoo, SENAITE, Keycloak
- **Integration Layer**: EIP 4.x FHIR Event Bus, RabbitMQ, IMIS-Connect
- **Insurance System**: openIMIS with FHIR R4 support

### Security & Access Control
- **SSO**: Keycloak for single sign-on
- **Data Segregation**: OpenMRS Data Filter module for band-based visibility
- **API Security**: Service tokens for backend communication

## Project Structure

```
ozone-nigeria-hmo/
├── docs/                      # Project documentation
│   ├── architecture.md        # System architecture overview
│   ├── api/                   # API documentation
│   │   └── imis-connect-openapi.yaml  # OpenAPI specification
│   └── ops-runbooks/          # Operational runbooks
│       └── claims-playbook.md # Claims management guide
├── eip/                       # EIP routes for FHIR event publishing
│   └── routes/
│       ├── patient-registered.xml
│       └── encounter-created.xml
├── infra/                     # Infrastructure configuration
│   └── docker/
│       ├── docker-compose.override.yml  # Docker Compose overlay
│       ├── imis-connect.Dockerfile      # IMIS-Connect Dockerfile
│       └── rabbitmq/
│           └── definitions.json         # RabbitMQ definitions
├── odoo-modules/              # Odoo insurance claim module
│   └── insurance_claim/
│       ├── models/            # Odoo models for tariffs and claims
│       ├── views/             # Odoo views and menus
│       ├── __init__.py
│       └── __manifest__.py
├── openmrs/                   # OpenMRS configuration
│   └── config/
│       └── initializer/       # OpenMRS Initializer configuration
│           ├── datafilter/
│           │   └── security.json      # Data filter mappings
│           └── patient_attributes/
│               └── band_attribute.json # Patient band attribute
├── scripts/                   # Helper scripts
│   ├── dev-start.sh           # Development environment startup
│   └── seed-tariffs.sh        # Tariff data seeding
└── services/                  # Backend services
    └── imis-connect/          # IMIS-Connect Spring Boot microservice
        ├── src/main/java/
        ├── src/main/resources/
        └── pom.xml
```

## Getting Started

### Prerequisites
- Docker and Docker Compose
- Git
- Java 17 (for development)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd ozone-nigeria-hmo
   ```

2. **Start the development environment**
   ```bash
   ./scripts/dev-start.sh
   ```

3. **Seed initial tariff data**
   ```bash
   ./scripts/seed-tariffs.sh
   ```

### Services Available

| Service | URL |
|---------|-----|
| Odoo | http://localhost:8069 |
| OpenMRS | http://localhost:8080/openmrs |
| SENAITE | http://localhost:8081 |
| Keycloak | http://localhost:8082 |
| RabbitMQ Management | http://localhost:15672 (guest/guest) |
| IMIS-Connect API | http://localhost:8085/swagger-ui.html |

## Usage

### Tariff Management

1. **Band D Tariff**: Navigate to `Insurance > Tariffs > Band D Tariff` to manage the global tariff for all Band D hospitals
2. **A/B/C Facility Tariff**: Navigate to `Insurance > Tariffs > A/B/C Facility Tariff` to manage facility-specific tariffs

### Claim Management

1. **Create Claim**: Navigate to `Insurance > Claims > Insurance Claims` and click "Create"
2. **Add Items**: Add claim items with service codes and quantities
3. **Submit Claim**: Click "Submit Claim" to send the claim to openIMIS
4. **Track Status**: Click "Get Claim Status" to check the latest status from openIMIS

### Eligibility Check

1. **Check Eligibility**: From the claim form, click "Check Eligibility" to verify patient insurance coverage
2. **View Results**: The eligibility response will show the patient's band and coverage details

## Development

### Building IMIS-Connect
```bash
cd services/imis-connect
mvn clean package
```

### Running Tests
```bash
cd services/imis-connect
mvn test
```

### Updating Odoo Module
```bash
# Update module in Odoo
./odoo-bin -c odoo.conf -u insurance_claim -i
```

## Architecture

### Tariff Resolution Algorithm

```python
def get_price(service_code, band, facility_uuid=None):
    if band == 'D':
        price = TariffD.get(service_code)  # uniform price for all Band D hospitals
    else:
        if not facility_uuid:
            raise ValueError('Facility UUID required for Bands A/B/C')
        price = TariffABC.get(service_code, band, facility_uuid)
    
    return price
```

### Band Visibility Rules

| User Band | Visible Bands |
|-----------|----------------|
| A         | A, B, C, D     |
| B         | B, C, D        |
| C         | C, D           |
| D         | D              |

## References

- Ozone HIS: [docs.ozone-his.com](https://docs.ozone-his.com)
- EIP 4.0: [talk.openmrs.org](https://talk.openmrs.org)
- OpenMRS Data Filter: [openmrs.atlassian.net](https://openmrs.atlassian.net)
- openIMIS FHIR: [fhir.openimis.org](https://fhir.openimis.org)
- RabbitMQ: [rabbitmq.com](https://rabbitmq.com)
- SENAITE: [senaite.com](https://senaite.com)

## License

This project is licensed under the MIT License. See the LICENSE file for more information.
