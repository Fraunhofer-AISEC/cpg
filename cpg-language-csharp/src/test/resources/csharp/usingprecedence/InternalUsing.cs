// The `Logger` inferred for ExternalUsing.cs must not shadow `App.Logging.Logger` here.
using App.Logging;

namespace App.Internal
{
    public class UsesInternalLogger
    {
        public Logger logger;
    }
}
