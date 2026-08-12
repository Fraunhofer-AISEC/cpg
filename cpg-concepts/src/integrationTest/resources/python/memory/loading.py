import loader

# simulate a loader. It will load the module "simple" and return an "SimpleImplClass" object
bar = loader.Loader("simple").impl

# bar should now be of type "SimpleImplClass"
bar.foo()
