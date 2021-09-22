// ------------------------------------------------------------
//  Copyright (c) Microsoft Corporation.  All rights reserved.
//  Licensed under the MIT License (MIT). See License.txt in the repo root for license information.
// ------------------------------------------------------------

namespace JepsenAPI.Controllers
{
    using System;
    using System.Collections.Generic;
    using System.Fabric;
    using System.Fabric.Query;
    using System.Linq;
    using System.Net.Http;
    using System.Net.Http.Headers;
    using System.Text;
    using System.Threading.Tasks;
    using Microsoft.AspNetCore.Mvc;
    using Newtonsoft.Json;

    [Produces("application/json")]
    [Route("api/[controller]")]
    public class ReliableConcurrentQueueController : Controller
    {
        private readonly HttpClient httpClient;
        private readonly FabricClient fabricClient;
        private readonly string reverseProxyBaseUri;
        private readonly StatelessServiceContext serviceContext;

        public ReliableConcurrentQueueController(HttpClient httpClient, StatelessServiceContext context, FabricClient fabricClient)
        {
            this.fabricClient = fabricClient;
            this.httpClient = httpClient;
            this.serviceContext = context;
            this.reverseProxyBaseUri = Environment.GetEnvironmentVariable("ReverseProxyBaseUri");
        }

        // GET: api/
        [HttpGet("")]
        public async Task<IActionResult> Get()
        {
            Uri serviceName = JepsenAPI.GetJepsenAPIStoreServiceName(this.serviceContext);
            Uri proxyAddress = this.GetProxyAddress(serviceName);

            ServicePartitionList partitions = await this.fabricClient.QueryManager.GetPartitionListAsync(serviceName);

            KeyValuePair<string, long> result = new KeyValuePair<string, long>();

            foreach (Partition partition in partitions)
            {
                string proxyUrl =
                    $"{proxyAddress}api/ReliableConcurrentQueue?PartitionKey={((Int64RangePartitionInformation)partition.PartitionInformation).LowKey}&PartitionKind=Int64Range";

                using (HttpResponseMessage response = await this.httpClient.GetAsync(proxyUrl))
                {
                    if (response.StatusCode != System.Net.HttpStatusCode.OK)
                    {
                        continue;
                    }

                    result = (JsonConvert.DeserializeObject<KeyValuePair<string, long>>(await response.Content.ReadAsStringAsync()));
                }
            }

            return this.Json(result);
        }

        // PUT: api/Votes/name
        [HttpPut("{key}")]
        public async Task<IActionResult> Put(string key)
        {
            Uri serviceName = JepsenAPI.GetJepsenAPIStoreServiceName(this.serviceContext);
            Uri proxyAddress = this.GetProxyAddress(serviceName);
            long partitionKey = this.GetPartitionKey(key);
            string proxyUrl = $"{proxyAddress}api/ReliableConcurrentQueue/{key}?PartitionKey={partitionKey}&PartitionKind=Int64Range";

            StringContent putContent = new StringContent($"{{ 'key' : '{key}' }}", Encoding.UTF8, "application/json");
            putContent.Headers.ContentType = new MediaTypeHeaderValue("application/json");

            using (HttpResponseMessage response = await this.httpClient.PutAsync(proxyUrl, putContent))
            {
                return new ContentResult()
                {
                    StatusCode = (int)response.StatusCode,
                    Content = await response.Content.ReadAsStringAsync()
                };
            }
        }

        // DELETE: api/Votes/name
        [HttpDelete("")]
        public async Task<IActionResult> Delete()
        {
            Uri serviceName = JepsenAPI.GetJepsenAPIStoreServiceName(this.serviceContext);
            Uri proxyAddress = this.GetProxyAddress(serviceName);
            ServicePartitionList partitions = await this.fabricClient.QueryManager.GetPartitionListAsync(serviceName);

            string proxyUrl = $"{proxyAddress}api/ReliableConcurrentQueue/?PartitionKey=0&PartitionKind=Int64Range";
            KeyValuePair<string, string> result = new KeyValuePair<string, string>();
            using (HttpResponseMessage response = await this.httpClient.DeleteAsync(proxyUrl))
            {
                if (response.StatusCode != System.Net.HttpStatusCode.OK)
                {
                    return this.StatusCode((int)response.StatusCode);
                }
                result = (JsonConvert.DeserializeObject<KeyValuePair<string, string>>(await response.Content.ReadAsStringAsync()));
            }
            return this.Json(result);
        }


        /// <summary>
        /// Constructs a reverse proxy URL for a given service.
        /// Example: http://localhost:19081/VotingApplication/VotingData/
        /// </summary>
        /// <param name="serviceName"></param>
        /// <returns></returns>
        private Uri GetProxyAddress(Uri serviceName)
        {
            return new Uri($"{this.reverseProxyBaseUri}");
            //return new Uri($"{this.reverseProxyBaseUri}{serviceName.AbsolutePath}");
        }

        /// <summary>
        /// Creates a partition key from the given name.
        /// Uses the zero-based numeric position in the alphabet of the first letter of the name (0-25).
        /// </summary>
        /// <param name="name"></param>
        /// <returns></returns>
        private long GetPartitionKey(string name)
        {
            return Char.ToUpper(name.First()) - 'A';
        }
    }
}