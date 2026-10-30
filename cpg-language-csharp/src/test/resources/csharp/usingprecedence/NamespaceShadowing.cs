// A type of the surrounding namespace shadows a type with the same name that is imported with a
// using directive.
using App.Logging;

namespace App.Core
{
    public class Logger
    {
    }

    public class UsesCoreLogger
    {
        public Logger logger;
    }
}
