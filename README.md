# PMC2Database
Puts PubMed Central documents (XML) into the database with metadata (title, PMCid,PMid, abstact, journal name, publication year...)

## What is PubMed Central==
PubMed Central® (PMC) is a free full-text archive of biomedical and life sciences journal literature at the U.S. National Institutes of Health's National Library of Medicine (NIH/NLM).

[https://www.ncbi.nlm.nih.gov/pmc/](https://www.ncbi.nlm.nih.gov/pmc/)

## How to run
Firstly download PubMed Central (PMC) files in XML format. You can do it from [ftp://ftp.ncbi.nlm.nih.gov/pub/pmc/oa_bulk/](ftp://ftp.ncbi.nlm.nih.gov/pub/pmc/oa_bulk/).

Unpack the tar.gz files. On linux you can use the following command:
```
for file in *.tar.gz; do tar -zxf $file; done
```
Or you can use 7zip on Windows.

Delete .tar.gz files.


Create mySQL database (you can also use existing)

Run pmc2017.sql file over your database. This will create table calle "pmc_articles_2017" in which PMC documents will be stored. The table definition is like:

```
CREATE TABLE `pmc_articles_2017` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `PMCid` int(11) DEFAULT NULL,
  `Title` varchar(500) DEFAULT NULL,
  `PMid` int(11) DEFAULT NULL,
  `Long_abstract` mediumtext,
  `Short_Abstract` mediumtext,
  `XML` longtext,
  `publisher_name` varchar(500) DEFAULT NULL,
  `publisher_loc` varchar(450) DEFAULT NULL,
  `journal_name` varchar(450) DEFAULT NULL,
  `year` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
)
```

Once you created a table, edit file called "database". The file should be containing database details (host, port, username and password). 

Once done, you can run the PMC2Database and populate your database. It can be done using the following command:
```
java -jar PMC2Database.jar path/to/unpacked/PMCfiles
```


## Author
Nikola Milosevic
