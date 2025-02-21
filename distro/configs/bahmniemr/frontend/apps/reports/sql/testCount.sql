SELECT DATE(lastupdated) AS "Date",
COUNT(id) AS "Number of Test Results Captured"
FROM result
WHERE DATE(lastupdated) BETWEEN '#startDate#' AND '#endDate#'
GROUP BY DATE(lastupdated)
ORDER BY DATE(lastupdated) ASC;
