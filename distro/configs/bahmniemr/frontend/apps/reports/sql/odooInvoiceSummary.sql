SELECT 
    DATE(invoice_date) AS "Date",
    COUNT(id) AS "Number of Invoices",
    SUM((amount_total - discount) + round_off_amount) AS "Total Invoice Value"
FROM 
    account_move
WHERE 
    move_type = 'out_invoice'
    AND state = 'posted'
    AND DATE(invoice_date) BETWEEN '#startDate#' AND '#endDate#'
GROUP BY 
    DATE(invoice_date)
ORDER BY 
    invoice_date ASC;
