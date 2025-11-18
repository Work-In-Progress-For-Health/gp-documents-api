using System.Net.Sockets;
using System.Text;

namespace Uk.HealthTechWales.GpPractice.Services;

public class ClamAVService : IClamAVService
{
    private readonly ILogger<ClamAVService> _logger;
    private readonly string _clamAvHost;
    private readonly int _clamAvPort;
    private readonly int _timeout;

    public ClamAVService(IConfiguration configuration, ILogger<ClamAVService> logger)
    {
        _logger = logger;
        _clamAvHost = configuration["ClamAV:Host"] ?? "localhost";
        _clamAvPort = int.Parse(configuration["ClamAV:Port"] ?? "3310");
        _timeout = int.Parse(configuration["ClamAV:Timeout"] ?? "5000");
    }

    public async Task<ScanResult> ScanDocumentAsync(string base64Data)
    {
        try
        {
            var documentBytes = Convert.FromBase64String(base64Data);
            return await ScanFileAsync(documentBytes);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error decoding base64 or scanning document");
            return ScanResult.CreateError($"Failed to scan document: {ex.Message}");
        }
    }

    public async Task<ScanResult> ScanFileAsync(byte[] fileData)
    {
        _logger.LogInformation("Connecting to ClamAV at {Host}:{Port}", _clamAvHost, _clamAvPort);

        try
        {
            using var client = new TcpClient();
            await client.ConnectAsync(_clamAvHost, _clamAvPort);
            client.ReceiveTimeout = _timeout;
            client.SendTimeout = _timeout;

            await using var stream = client.GetStream();

            // Send INSTREAM command
            var instreamCommand = Encoding.UTF8.GetBytes("zINSTREAM\0");
            await stream.WriteAsync(instreamCommand);
            await stream.FlushAsync();

            // Send file data in chunks
            const int chunkSize = 2048;
            int offset = 0;

            while (offset < fileData.Length)
            {
                int length = Math.Min(chunkSize, fileData.Length - offset);

                // Send chunk size (4 bytes, network byte order / big-endian)
                var sizeBytes = BitConverter.GetBytes(length);
                if (BitConverter.IsLittleEndian)
                {
                    Array.Reverse(sizeBytes);
                }
                await stream.WriteAsync(sizeBytes);

                // Send chunk data
                await stream.WriteAsync(fileData.AsMemory(offset, length));
                offset += length;
            }

            // Send zero-length chunk to indicate end of stream
            await stream.WriteAsync(new byte[] { 0, 0, 0, 0 });
            await stream.FlushAsync();

            // Read response
            using var reader = new StreamReader(stream, Encoding.UTF8);
            var response = await reader.ReadLineAsync();

            _logger.LogInformation("ClamAV response: {Response}", response);

            return ParseResponse(response);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error connecting to ClamAV");
            return ScanResult.CreateError($"Failed to scan file with ClamAV: {ex.Message}");
        }
    }

    public async Task<bool> PingAsync()
    {
        try
        {
            using var client = new TcpClient();
            await client.ConnectAsync(_clamAvHost, _clamAvPort);
            client.ReceiveTimeout = _timeout;
            client.SendTimeout = _timeout;

            await using var stream = client.GetStream();

            var pingCommand = Encoding.UTF8.GetBytes("zPING\0");
            await stream.WriteAsync(pingCommand);
            await stream.FlushAsync();

            using var reader = new StreamReader(stream, Encoding.UTF8);
            var response = await reader.ReadLineAsync();

            _logger.LogDebug("ClamAV PING response: {Response}", response);
            return response?.Replace("\0", "").Trim() == "PONG";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to ping ClamAV");
            return false;
        }
    }

    private ScanResult ParseResponse(string? response)
    {
        if (string.IsNullOrEmpty(response))
        {
            return ScanResult.CreateError("Unknown - empty response from ClamAV");
        }

        // Remove "stream: " prefix if present
        var result = response.Replace("stream: ", "").Trim();

        if (result.Equals("OK", StringComparison.OrdinalIgnoreCase))
        {
            _logger.LogInformation("File is clean");
            return ScanResult.CreateClean();
        }

        if (result.Contains("FOUND", StringComparison.OrdinalIgnoreCase))
        {
            // Extract virus name
            var virusName = result.Replace(" FOUND", "", StringComparison.OrdinalIgnoreCase).Trim();
            _logger.LogWarning("Virus detected: {VirusName}", virusName);
            return ScanResult.CreateInfected(virusName);
        }

        _logger.LogError("Unexpected ClamAV response: {Response}", response);
        return ScanResult.CreateError($"Unexpected response: {response}");
    }
}
