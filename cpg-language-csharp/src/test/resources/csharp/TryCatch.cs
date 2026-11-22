namespace Test
{
    class TryCatch
    {
        int TryCatchStmt(string s)
        {
            try
            {
                return Convert(s);
            }
            catch (FormatException e)
            {
                Log(e);
                return -1;
            }
        }

        void TryCatchFinally()
        {
            try
            {
                Open();
            }
            catch (IOException e)
            {
                Log(e);
            }
            finally
            {
                Close();
            }
        }

        void TryFinally()
        {
            try
            {
                Open();
            }
            finally
            {
                Close();
            }
        }

        void MultipleCatches()
        {
            try
            {
                Open();
            }
            catch (FormatException e)
            {
                Log(e);
            }
            catch (IOException)
            {
                Fallback();
            }
            catch
            {
                Fallback();
            }
        }

        void CatchFilter()
        {
            try
            {
                Open();
            }
            catch (IOException e) when (e.Message == "retry")
            {
                Retry();
            }
        }

        void ThrowStmt()
        {
            throw new InvalidOperationException("nope");
        }

        void Rethrow()
        {
            try
            {
                Open();
            }
            catch (IOException)
            {
                throw;
            }
        }

        string ThrowExpr(string s)
        {
            return s ?? throw new ArgumentNullException("s");
        }

        int Convert(string s)
        {
            return 0;
        }

        void Log(object e) { }

        void Open() { }

        void Close() { }

        void Fallback() { }

        void Retry() { }
    }
}
