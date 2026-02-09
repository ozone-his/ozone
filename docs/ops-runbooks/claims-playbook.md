# Claims Management Playbook

## Overview

This playbook provides operational guidelines for managing insurance claims in the Ozone Nigeria HMO system. It covers claim creation, submission, status tracking, and troubleshooting.

## Prerequisites

- User must have appropriate permissions to access insurance modules
- Odoo and IMIS-Connect services must be running
- Internet connectivity to openIMIS

## Claim Creation

### Step 1: Create New Claim

1. Navigate to **Insurance > Claims > Insurance Claims**
2. Click **Create** to start a new claim
3. Fill in the following information:
   - **Patient ID**: OpenMRS patient identifier
   - **Insuree ID**: Insurance card or policy number
   - **Patient Band**: Select from A, B, C, or D
   - **Facility UUID**: Unique facility identifier

### Step 2: Add Claim Items

1. In the **Claim Items** tab, click **Add a line**
2. Enter the following details for each item:
   - **Service Code**: Standard service code (e.g., CONS-OPD)
   - **Description**: Detailed service description
   - **Quantity**: Number of units
   - **Unit Price**: Price per unit (will be calculated based on band and facility)

### Step 3: Review and Validate

1. Check the calculated **Total Amount**
2. Verify all claim items and patient information
3. Click **Save** to create the claim in draft status

## Claim Submission

### Step 1: Check Eligibility (Optional)

1. Open the claim form
2. Click **Check Eligibility** button
3. Verify the eligibility response from openIMIS
4. Confirm patient's band and coverage details

### Step 2: Submit Claim

1. Ensure claim status is **Draft**
2. Click **Submit Claim** button
3. Confirm the submission in the dialog box
4. System will display the openIMIS claim identifier

### Step 3: Monitor Submission Status

1. The claim status will change to **Submitted**
2. Check the **openIMIS Claim ID** field for the claim reference
3. Use this ID to track claim status with the insurance provider

## Claim Status Tracking

### View Claim Status

1. Navigate to **Insurance > Claims > Insurance Claims**
2. Open the claim you want to check
3. Click **Get Claim Status** button
4. The system will display the latest status from openIMIS

### Status Definitions

- **Draft**: Claim is being prepared (not submitted)
- **Submitted**: Claim has been sent to openIMIS
- **Processed**: Claim is being reviewed by the insurance
- **Approved**: Claim has been approved for payment
- **Rejected**: Claim has been rejected (see rejection reason)

## Tariff Management

### Band D Tariff (Global)

1. Navigate to **Insurance > Tariffs > Band D Tariff**
2. Click **Create** to add a new service tariff
3. Fill in:
   - **Service Code**: Standard service code
   - **Price (NGN)**: Uniform price for all Band D hospitals
   - **Effective Date**: Date the tariff becomes active

### A/B/C Facility Tariff (Facility-Specific)

1. Navigate to **Insurance > Tariffs > A/B/C Facility Tariff**
2. Click **Create** to add a new facility-specific tariff
3. Fill in:
   - **Service Code**: Standard service code
   - **Band**: Select A, B, or C
   - **Facility UUID**: Target facility identifier
   - **Price (NGN)**: Facility-specific price
   - **Effective Date**: Date the tariff becomes active

## Troubleshooting

### Claim Submission Errors

**Issue**: Claim submission fails with "Connection Error"

**Solution**:
1. Check if IMIS-Connect service is running
2. Verify network connectivity to openIMIS
3. Check openIMIS service status
4. Review error logs in Odoo and IMIS-Connect

**Issue**: Claim is rejected with "Invalid Service Code"

**Solution**:
1. Verify the service code exists in the tariff table
2. Check the effective date of the tariff
3. Ensure the tariff is applicable to the patient's band
4. Contact the insurance provider for valid service codes

### Tariff Resolution Issues

**Issue**: Incorrect price is calculated

**Solution**:
1. Check if the service code exists in the correct tariff table
2. Verify the patient's band classification
3. Check the facility UUID for A/B/C band claims
4. Ensure the tariff's effective date is valid

### Eligibility Check Problems

**Issue**: Eligibility check returns "Insuree Not Found"

**Solution**:
1. Verify the insuree ID is correct
2. Check if the patient exists in openIMIS
3. Confirm the patient's coverage is active
4. Contact the insurance provider to verify the policy

## Performance Optimization

### Batch Processing

For high volume claim submission:
1. Use the **Batch Claim Submission** wizard
2. Prepare claims in Excel format
3. Validate claims before submission
4. Monitor batch processing status

### Scheduled Status Checks

Set up automatic claim status polling:
1. Navigate to **Settings > Technical > Scheduled Actions**
2. Create a new action for claim status updates
3. Configure the frequency (e.g., every 6 hours)
4. Monitor the action logs for any errors

## Reporting

### Claim Analytics

1. Navigate to **Insurance > Reports > Claim Analytics**
2. Filter by status, date range, band, or facility
3. Generate reports for claim volumes and amounts
4. Export reports in PDF or Excel format

### Tariff Comparison

1. Navigate to **Insurance > Reports > Tariff Comparison**
2. Compare prices across bands and facilities
3. Identify discrepancies in tariff structures
4. Generate reconciliation reports

## Security Considerations

### Access Control

- Ensure users have appropriate role permissions
- Review access logs regularly
- Restrict claim deletion to authorized users

### Data Protection

- Encrypt all communication with openIMIS
- Mask sensitive patient information in reports
- Implement audit trails for all claim transactions

## Contact Information

For support and assistance:
- **Help Desk**: support@ozone-his.com
- **Documentation**: https://docs.ozone-his.com
- **Issue Tracker**: https://github.com/ozone-his/ozone-nigeria-hmo/issues
