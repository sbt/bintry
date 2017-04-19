### releasing steps

```
jenv shell 1.7
sbt
> clean
> ++2.10.6
> publishSigned
> ++2.11.11
> publishSigned
> exit
jenv shell 1.8
sbt
> ++2.12.2
> publishSigned
```
