// `External.Lib` is not part of the analyzed sources, so `Logger` cannot be resolved here and is
// inferred.
using External.Lib;

namespace App.External
{
    public class UsesExternalLogger
    {
        public Logger logger;
    }
}
