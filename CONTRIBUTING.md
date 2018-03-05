### releasing steps

```
jenv shell 1.7
sbt
> clean
> ++2.10.7
> publishSigned
> ++2.11.12
> publishSigned
> exit
jenv shell 1.8
sbt
> ++2.12.4
> publishSigned
```
