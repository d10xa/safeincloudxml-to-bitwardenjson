
## build

```./sbt assembly```

## run

```
java -jar target/scala-2.13/safeincloudxml-to-bitwardenjson.jar sic-to-bw --help
```

## help

```
Subcommands:
    sic-to-bw
        convert SafeInCloud.xml to bitwarden.json
    plain-print
        print database in plain comparable format
```

```
Usage:  sic-to-bw --input <path> --output <path> [--bw-with-ids <path>]
   
convert SafeInCloud.xml to bitwarden.json

Options and flags:
    --help
        Display this help text.
    --input <path>
        sic input xml
    --output <path>
        bw output json
    --bw-with-ids <path>
        bw backup with folders (use ids from backup)


```

```
Usage:
     plain-print --input-bw <path> --output <path>
     plain-print --input-sic <path> --output <path>

print database in plain comparable format

Options and flags:
    --help
        Display this help text.
    --input-bw <path>
        bitwarden json file
    --output <path>
        result output file
    --input-sic <path>
        SafeInCloud xml file
```