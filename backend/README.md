# Images
`GET "/images/"`

`POST "/images/add"`
```
{
    Reference: string
}
```

`POST "/images/update"`
```
{
    Reference: string
}
```

`DELETE "/images/remove"`
```
{
    Reference: string
}
```

# /Instances
`GET "/instances"`

`POST "/instances/start"`
```
{
    Name: string
    Reference: string
}
```

`POST "/instances/stop"`
```
{
    Name: string
    Reference: string
}
```

`POST "/instances/reset"`
```
{
    Name: string
    Reference: string
}
```

# Live Environment
`POST "/live/start"`
```
{
    Name: string
}
```

`POST "/live/stop"`
```
{
    Name: string
}
```

`POST "/live/reset"`
```
{
    Name: string
}
```